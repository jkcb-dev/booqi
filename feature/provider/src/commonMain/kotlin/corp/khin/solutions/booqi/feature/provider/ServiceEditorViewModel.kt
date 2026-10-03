package corp.khin.solutions.booqi.feature.provider

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import corp.khin.solutions.booqi.core.common.DomainError
import corp.khin.solutions.booqi.core.common.DomainResult
import corp.khin.solutions.booqi.domain.model.Service
import corp.khin.solutions.booqi.domain.model.ServiceDetails
import corp.khin.solutions.booqi.domain.usecase.AgregarServicioUseCase
import corp.khin.solutions.booqi.domain.usecase.EditarServicioUseCase
import corp.khin.solutions.booqi.domain.usecase.ObtenerServicioUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * P5 — add (`serviceId == null`) or edit (`serviceId != null`) a service, for
 * `Destination.ServiceEditor(serviceId)`. Talks to the domain only through
 * [AgregarServicioUseCase]/[EditarServicioUseCase]/[ObtenerServicioUseCase].
 *
 * **No init-time load.** The instance can outlive a visit to the editor (not destination-scoped,
 * see docs/DEVELOPMENT.md), so the screen sends [ServiceEditorAction.Start] on every entry; that
 * resets all form state and (re)loads for the given id. [initialServiceId] only shapes the very
 * first frame before `Start` arrives.
 *
 * [providerId] defaults to the TEMPORARY placeholder (see [TEMPORARY_PROVIDER_ID]).
 */
class ServiceEditorViewModel(
    initialServiceId: String?,
    private val agregarServicio: AgregarServicioUseCase,
    private val editarServicio: EditarServicioUseCase,
    private val obtenerServicio: ObtenerServicioUseCase,
    private val providerId: String = TEMPORARY_PROVIDER_ID,
) : ViewModel() {

    private var serviceId: String? = initialServiceId
    private var loadJob: Job? = null

    private val _state = MutableStateFlow(initialStateFor(initialServiceId))
    val state: StateFlow<ServiceEditorUiState> = _state.asStateFlow()

    private val _events = Channel<ServiceEditorEvent>()
    val events = _events.receiveAsFlow()

    fun onAction(action: ServiceEditorAction) {
        when (action) {
            is ServiceEditorAction.Start -> start(action.serviceId)
            is ServiceEditorAction.PhotoUrlChanged ->
                _state.update { it.copy(photoUrlInput = action.value, photoError = null) }
            is ServiceEditorAction.TitleChanged -> _state.update { it.copy(titleInput = action.value) }
            is ServiceEditorAction.DescriptionChanged -> _state.update { it.copy(descriptionInput = action.value) }
            is ServiceEditorAction.PriceChanged ->
                _state.update { it.copy(priceInput = action.value, priceError = null) }
            is ServiceEditorAction.DurationChanged ->
                _state.update { it.copy(durationInput = action.value, durationError = null) }
            is ServiceEditorAction.ModalityChanged -> _state.update { it.copy(modality = action.modality) }
            ServiceEditorAction.Save -> save()
        }
    }

    private fun initialStateFor(id: String?) = ServiceEditorUiState(serviceId = id, isLoading = id != null)

    /** Clean slate for [id]: drops any previous visit's inputs/errors and cancels a stale load. */
    private fun start(id: String?) {
        loadJob?.cancel()
        serviceId = id
        _state.value = initialStateFor(id)
        if (id != null) loadService(id)
    }

    /** Escenario: "El Proveedor consulta un Servicio para editarlo" (and its "no existe" twin —
     * `NotFound` lands in [ServiceEditorUiState.error], which the screen renders as a
     * "servicio no encontrado" state with nothing to save). */
    private fun loadService(id: String) {
        loadJob = viewModelScope.launch {
            when (val result = obtenerServicio(id)) {
                is DomainResult.Success -> _state.update { it.preloadedFrom(result.value) }
                is DomainResult.Failure -> {
                    _state.update { it.copy(isLoading = false, error = result.error) }
                    _events.send(ServiceEditorEvent.ShowError(result.error.describe(NOT_FOUND_MESSAGE)))
                }
            }
        }
    }

    private fun ServiceEditorUiState.preloadedFrom(service: Service) = copy(
        isLoading = false,
        photoUrlInput = service.photoUrl,
        titleInput = service.title,
        descriptionInput = service.description,
        priceInput = formatPriceInput(service.priceCents),
        durationInput = service.durationMinutes.toString(),
        modality = service.modality,
    )

    /** Escenarios: "El Proveedor agrega un nuevo Servicio" / "...sin foto" / "...edita un Servicio
     * existente". Price/duration that don't parse are form errors on their own fields; the
     * foto-obligatoria rule is the use case's, and comes back as `InvalidInput`, shown under the
     * photo field — never a crash or a generic error event. */
    private fun save() {
        val current = _state.value
        if (current.isSaving || current.isLoading) return
        val priceCents = parsePriceCents(current.priceInput)
        val durationMinutes = parseDurationMinutes(current.durationInput)
        if (priceCents == null || durationMinutes == null) {
            _state.update {
                it.copy(
                    // The use case never runs while the numbers are invalid, so to show every
                    // error at once the photo-required rule it enforces is mirrored here for this
                    // path only. The use case stays the authority: its InvalidInput is still what
                    // is rendered when a valid-number submit reaches it (see onSaveFailed).
                    photoError = if (current.photoUrlInput.isBlank()) PHOTO_REQUIRED_ERROR else null,
                    priceError = if (priceCents == null) PRICE_ERROR else null,
                    durationError = if (durationMinutes == null) DURATION_ERROR else null,
                )
            }
            return
        }
        val details = ServiceDetails(
            title = current.titleInput,
            photoUrl = current.photoUrlInput,
            description = current.descriptionInput,
            priceCents = priceCents,
            durationMinutes = durationMinutes,
            modality = current.modality,
        )
        val targetId = serviceId
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true, photoError = null, priceError = null, durationError = null) }
            val result = if (targetId == null) {
                agregarServicio(providerId, details)
            } else {
                editarServicio(targetId, details)
            }
            when (result) {
                is DomainResult.Success -> {
                    _state.update { it.copy(isSaving = false, error = null) }
                    _events.send(ServiceEditorEvent.Saved)
                }
                is DomainResult.Failure -> onSaveFailed(result.error)
            }
        }
    }

    private suspend fun onSaveFailed(error: DomainError) {
        if (error is DomainError.InvalidInput) {
            // The only input rule the use cases enforce today is "la foto es obligatoria".
            _state.update { it.copy(isSaving = false, photoError = error.message) }
        } else {
            _state.update { it.copy(isSaving = false, error = error) }
            _events.send(ServiceEditorEvent.ShowError(error.describe(NOT_FOUND_MESSAGE)))
        }
    }

    private companion object {
        const val NOT_FOUND_MESSAGE = "No se encontró el servicio"
        const val PHOTO_REQUIRED_ERROR = "La foto es obligatoria"
        const val PRICE_ERROR = "Ingresá un precio válido, por ejemplo 12.50"
        const val DURATION_ERROR = "Ingresá la duración en minutos, por ejemplo 45"
    }
}
