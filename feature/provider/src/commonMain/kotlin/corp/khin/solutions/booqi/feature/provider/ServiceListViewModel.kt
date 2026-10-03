package corp.khin.solutions.booqi.feature.provider

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import corp.khin.solutions.booqi.core.common.DomainResult
import corp.khin.solutions.booqi.domain.model.Service
import corp.khin.solutions.booqi.domain.usecase.DeshabilitarServicioUseCase
import corp.khin.solutions.booqi.domain.usecase.HabilitarServicioUseCase
import corp.khin.solutions.booqi.domain.usecase.ObtenerServiciosDelProveedorUseCase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * P4 — the Provider's service list. Talks to the domain only through
 * [ObtenerServiciosDelProveedorUseCase]/[DeshabilitarServicioUseCase]/[HabilitarServicioUseCase].
 * Single [onAction] entry point, same shape as [ProviderProfileViewModel].
 *
 * **No init-time load.** ViewModels are not destination-scoped by our simple navigator (see
 * docs/DEVELOPMENT.md), so the same instance is reused each time the list re-enters composition,
 * e.g. when coming back from the editor. [ServiceListScreen] therefore sends
 * [ServiceListAction.Refresh] every time it enters composition.
 *
 * [providerId] defaults to the TEMPORARY placeholder (see [TEMPORARY_PROVIDER_ID]); wiring can
 * pass the real one once Identity exists.
 */
class ServiceListViewModel(
    private val obtenerServicios: ObtenerServiciosDelProveedorUseCase,
    private val deshabilitarServicio: DeshabilitarServicioUseCase,
    private val habilitarServicio: HabilitarServicioUseCase,
    private val providerId: String = TEMPORARY_PROVIDER_ID,
) : ViewModel() {

    // Starts in the loading state so the first frame before ServiceListScreen's Refresh never
    // flashes the "no tenés servicios" empty state.
    private val _state = MutableStateFlow(ServiceListUiState(isLoading = true))
    val state: StateFlow<ServiceListUiState> = _state.asStateFlow()

    private val _events = Channel<ServiceListEvent>()
    val events = _events.receiveAsFlow()

    fun onAction(action: ServiceListAction) {
        when (action) {
            ServiceListAction.Refresh -> load()
            is ServiceListAction.SetServiceEnabled -> setEnabled(action.serviceId, action.enabled)
            ServiceListAction.AddServiceClicked -> emit(ServiceListEvent.NavigateToAddService)
            is ServiceListAction.EditServiceClicked ->
                emit(ServiceListEvent.NavigateToEditService(action.serviceId))
        }
    }

    private fun emit(event: ServiceListEvent) {
        viewModelScope.launch { _events.send(event) }
    }

    /** Escenario: "El Proveedor ve todos sus Servicios, incluidos los deshabilitados". */
    private fun load() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            when (val result = obtenerServicios(providerId)) {
                is DomainResult.Success -> _state.update {
                    it.copy(isLoading = false, services = result.value)
                }
                is DomainResult.Failure -> {
                    _state.update { it.copy(isLoading = false, error = result.error) }
                    _events.send(ServiceListEvent.ShowError(result.error.describe(NOT_FOUND_MESSAGE)))
                }
            }
        }
    }

    /** Escenarios: "El Proveedor deshabilita un Servicio" / "...re-habilita un Servicio
     * deshabilitado". The updated service coming back from the use case replaces the row in
     * place, so list order (creation order) never changes. */
    private fun setEnabled(serviceId: String, enabled: Boolean) {
        if (serviceId in _state.value.togglingServiceIds) return
        viewModelScope.launch {
            _state.update { it.copy(togglingServiceIds = it.togglingServiceIds + serviceId, error = null) }
            val result = if (enabled) habilitarServicio(serviceId) else deshabilitarServicio(serviceId)
            when (result) {
                is DomainResult.Success -> _state.update {
                    it.copy(
                        services = it.services.replaceById(result.value),
                        togglingServiceIds = it.togglingServiceIds - serviceId,
                    )
                }
                is DomainResult.Failure -> {
                    _state.update {
                        it.copy(togglingServiceIds = it.togglingServiceIds - serviceId, error = result.error)
                    }
                    _events.send(ServiceListEvent.ShowError(result.error.describe(NOT_FOUND_MESSAGE)))
                }
            }
        }
    }

    private fun List<Service>.replaceById(updated: Service): List<Service> =
        map { if (it.id == updated.id) updated else it }

    private companion object {
        const val NOT_FOUND_MESSAGE = "No se encontró el servicio"
    }
}
