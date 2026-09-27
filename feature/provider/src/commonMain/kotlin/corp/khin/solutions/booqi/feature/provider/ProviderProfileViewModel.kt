package corp.khin.solutions.booqi.feature.provider

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import corp.khin.solutions.booqi.core.common.DomainError
import corp.khin.solutions.booqi.core.common.DomainResult
import corp.khin.solutions.booqi.domain.model.DateRange
import corp.khin.solutions.booqi.domain.usecase.ActivarModoProveedorUseCase
import corp.khin.solutions.booqi.domain.usecase.CompletarPerfilDeProveedorUseCase
import corp.khin.solutions.booqi.domain.usecase.PausarPerfilUseCase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate

/**
 * Talks to the domain layer only through [ActivarModoProveedorUseCase]/
 * [CompletarPerfilDeProveedorUseCase]/[PausarPerfilUseCase] — never a repository or datasource
 * directly. Single [onAction] entry point keeps this reducer-shaped and easy to test without
 * touching Compose, matching `feature:browse`'s `BrowseViewModel` shape.
 *
 * Covers all of docs/domain/provider-flow.md § Grupo 1 in one ViewModel since
 * `Destination.ProviderProfileSetup` is one screen for both "activar modo" (P1) and "completar
 * perfil" (P2), and "pausar perfil" (P3) is reached as an in-screen action once the profile is
 * complete rather than a separate destination (see this PR's description for why).
 */
class ProviderProfileViewModel(
    private val activarModoProveedor: ActivarModoProveedorUseCase,
    private val completarPerfil: CompletarPerfilDeProveedorUseCase,
    private val pausarPerfil: PausarPerfilUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(ProviderProfileUiState())
    val state: StateFlow<ProviderProfileUiState> = _state.asStateFlow()

    private val _events = Channel<ProviderProfileEvent>()
    val events = _events.receiveAsFlow()

    fun onAction(action: ProviderProfileAction) {
        when (action) {
            is ProviderProfileAction.ActivateProviderMode -> activateProviderMode()
            is ProviderProfileAction.NameChanged -> _state.update { it.copy(nameInput = action.value) }
            is ProviderProfileAction.PhotoUrlChanged -> _state.update { it.copy(photoUrlInput = action.value) }
            is ProviderProfileAction.DescriptionChanged ->
                _state.update { it.copy(descriptionInput = action.value) }
            is ProviderProfileAction.LocationChanged ->
                _state.update { it.copy(locationInput = action.value, locationError = null) }
            is ProviderProfileAction.SaveProfile -> saveProfile()
            is ProviderProfileAction.ShowPauseSheet -> _state.update { it.copy(isPauseSheetVisible = true) }
            is ProviderProfileAction.DismissPauseSheet -> _state.update {
                it.copy(
                    isPauseSheetVisible = false,
                    pauseFrom = null,
                    pauseUntil = null,
                    pauseRangeError = null,
                )
            }
            is ProviderProfileAction.PauseFromChanged ->
                _state.update { it.copy(pauseFrom = action.date, pauseRangeError = null) }
            is ProviderProfileAction.PauseUntilChanged ->
                _state.update { it.copy(pauseUntil = action.date, pauseRangeError = null) }
            is ProviderProfileAction.ConfirmPause -> confirmPause()
            is ProviderProfileAction.ReactivateProfile -> reactivateProfile()
        }
    }

    /** Escenario: "Un usuario activa el modo Proveedor". Idempotent on the repository side, so
     * re-entering this screen and tapping Activar again is safe (returns the existing profile). */
    private fun activateProviderMode() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            when (val result = activarModoProveedor(CURRENT_USER_ID_PLACEHOLDER)) {
                is DomainResult.Success -> _state.update {
                    it.copy(
                        isLoading = false,
                        profile = result.value,
                        nameInput = result.value.name.orEmpty(),
                        photoUrlInput = result.value.photoUrl.orEmpty(),
                        descriptionInput = result.value.description.orEmpty(),
                        locationInput = result.value.location.orEmpty(),
                    )
                }
                is DomainResult.Failure -> {
                    _state.update { it.copy(isLoading = false, error = result.error) }
                    _events.send(ProviderProfileEvent.ShowError(result.error.describe()))
                }
            }
        }
    }

    /** Escenarios: "El Proveedor completa su perfil" / "...intenta completar el perfil sin
     * ubicación". The ubicación-obligatoria failure is surfaced as [ProviderProfileUiState
     * .locationError] on the form, never as a crash or a generic error event. */
    private fun saveProfile() {
        val profile = _state.value.profile ?: return
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true, locationError = null) }
            val current = _state.value
            when (
                val result = completarPerfil(
                    profileId = profile.id,
                    name = current.nameInput,
                    photoUrl = current.photoUrlInput,
                    description = current.descriptionInput,
                    location = current.locationInput,
                )
            ) {
                is DomainResult.Success -> {
                    _state.update { it.copy(isSaving = false, profile = result.value) }
                    _events.send(ProviderProfileEvent.ProfileSaved)
                }
                is DomainResult.Failure -> {
                    val error = result.error
                    if (error is DomainError.InvalidInput) {
                        _state.update { it.copy(isSaving = false, locationError = error.message) }
                    } else {
                        _state.update { it.copy(isSaving = false, error = error) }
                        _events.send(ProviderProfileEvent.ShowError(error.describe()))
                    }
                }
            }
        }
    }

    /** Escenario: "El Proveedor pausa su perfil por un rango de fechas". [DateRange]'s own
     * `start <= end` invariant is checked client-side first so an invalid range never reaches the
     * use case as a crash (`require` throwing) instead of a form error. */
    private fun confirmPause() {
        val profile = _state.value.profile ?: return
        val from = _state.value.pauseFrom
        val until = _state.value.pauseUntil
        val validationError = validatePauseRange(from, until)
        if (validationError != null) {
            _state.update { it.copy(pauseRangeError = validationError) }
            return
        }
        checkNotNull(from)
        checkNotNull(until)
        viewModelScope.launch {
            _state.update { it.copy(isPausing = true, pauseRangeError = null) }
            when (val result = pausarPerfil(profile.id, DateRange(from, until))) {
                is DomainResult.Success -> {
                    _state.update {
                        it.copy(
                            isPausing = false,
                            profile = result.value,
                            isPauseSheetVisible = false,
                            pauseFrom = null,
                            pauseUntil = null,
                        )
                    }
                    _events.send(ProviderProfileEvent.ProfilePaused)
                }
                is DomainResult.Failure -> {
                    _state.update { it.copy(isPausing = false, error = result.error) }
                    _events.send(ProviderProfileEvent.ShowError(result.error.describe()))
                }
            }
        }
    }

    /** Escenario: "El Proveedor reactiva su perfil antes de tiempo" — pausing with `null` clears
     * [corp.khin.solutions.booqi.domain.model.ProviderProfile.pausedRange] immediately. */
    private fun reactivateProfile() {
        val profile = _state.value.profile ?: return
        viewModelScope.launch {
            _state.update { it.copy(isPausing = true) }
            when (val result = pausarPerfil(profile.id, null)) {
                is DomainResult.Success -> {
                    _state.update { it.copy(isPausing = false, profile = result.value) }
                    _events.send(ProviderProfileEvent.ProfileReactivated)
                }
                is DomainResult.Failure -> {
                    _state.update { it.copy(isPausing = false, error = result.error) }
                    _events.send(ProviderProfileEvent.ShowError(result.error.describe()))
                }
            }
        }
    }

    private fun DomainError.describe(): String = when (this) {
        is DomainError.InvalidInput -> message
        is DomainError.Unknown -> message ?: "Ocurrió un error inesperado"
        DomainError.NoConnection -> "Sin conexión"
        DomainError.Timeout -> "La operación tardó demasiado"
        DomainError.NotFound -> "No se encontró el perfil"
        DomainError.Unauthorized -> "No autorizado"
    }

    private fun validatePauseRange(from: LocalDate?, until: LocalDate?): String? = when {
        from == null || until == null -> "Selecciona una fecha de inicio y de fin"
        from > until -> "La fecha de inicio debe ser anterior a la de fin"
        else -> null
    }

    private companion object {
        // TEMPORARY: there is no auth/session concept built yet in this codebase (Identity
        // bounded context is "Not built yet" per docs/DOMAIN.md). Hardcoded so the
        // screen/ViewModel/Koin wiring can be proven end-to-end, same spirit as the fake
        // datasources used elsewhere before a real backend/session existed. Replace with the real
        // signed-in User's id once Identity exists.
        const val CURRENT_USER_ID_PLACEHOLDER = "user-placeholder-temp"
    }
}
