package corp.khin.solutions.booqi.feature.browse

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import corp.khin.solutions.booqi.core.common.DomainResult
import corp.khin.solutions.booqi.domain.usecase.VerDetalleServicioUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * C3 — a Service's detail. Talks to the domain only through [VerDetalleServicioUseCase]
 * (escenario "El Cliente ve el detalle de un Servicio"). A missing, disabled or
 * incomplete-profile Service comes back `NotFound`, which the screen shows as "Servicio no
 * encontrado".
 *
 * **No init-time load, no per-destination instance.** The navigator doesn't scope ViewModels (see
 * docs/DEVELOPMENT.md), so the same instance serves every Service. [ServiceDetailScreen] sends
 * [ServiceDetailAction.Start] for its id on every entry, which wipes the previous Service and
 * loads the new one.
 */
class ServiceDetailViewModel(
    private val verDetalleServicio: VerDetalleServicioUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(ServiceDetailUiState())
    val state: StateFlow<ServiceDetailUiState> = _state.asStateFlow()

    private val _events = Channel<ServiceDetailEvent>()
    val events = _events.receiveAsFlow()

    private var loadJob: Job? = null

    fun onAction(action: ServiceDetailAction) {
        when (action) {
            is ServiceDetailAction.Start -> load(action.serviceId)
            ServiceDetailAction.Retry -> _state.value.serviceId?.let(::load)
            ServiceDetailAction.ProviderClicked -> _state.value.detail?.let {
                emit(ServiceDetailEvent.NavigateToProvider(it.provider.id))
            }
            ServiceDetailAction.BookClicked -> _state.value.detail?.let {
                emit(ServiceDetailEvent.NavigateToBooking(it.service.id, it.provider.id))
            }
            ServiceDetailAction.BackClicked -> emit(ServiceDetailEvent.NavigateBack)
        }
    }

    private fun emit(event: ServiceDetailEvent) {
        viewModelScope.launch { _events.send(event) }
    }

    private fun load(serviceId: String) {
        loadJob?.cancel()
        _state.value = ServiceDetailUiState(serviceId = serviceId, isLoading = true)
        loadJob = viewModelScope.launch {
            when (val result = verDetalleServicio(serviceId)) {
                is DomainResult.Success -> _state.update { it.copy(isLoading = false, detail = result.value) }
                is DomainResult.Failure -> _state.update { it.copy(isLoading = false, error = result.error) }
            }
        }
    }
}
