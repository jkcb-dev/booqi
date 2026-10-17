package corp.khin.solutions.booqi.feature.browse

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import corp.khin.solutions.booqi.core.common.DomainResult
import corp.khin.solutions.booqi.domain.usecase.RequerirCuentaVerificadaUseCase
import corp.khin.solutions.booqi.domain.usecase.VerPerfilProveedorUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * C4 — a Provider's public page (`Destination.ProviderProfileView`). Talks to the domain only
 * through [VerPerfilProveedorUseCase] (escenario "El Cliente ve el perfil completo de un
 * Proveedor": bio, location, all active Services, overall rating and individual reviews). A
 * missing or incomplete profile is `NotFound`.
 *
 * Not named `ProviderProfileViewModel` on purpose: `feature:provider` already has one (the
 * Provider editing their *own* profile) and both end up imported side by side in `App.kt`.
 * Same reuse rule as [ServiceDetailViewModel]: [ProviderPublicProfileScreen] sends
 * [ProviderPublicProfileAction.Start] on every entry.
 */
class ProviderPublicProfileViewModel(
    private val verPerfilProveedor: VerPerfilProveedorUseCase,
    private val requerirCuentaVerificada: RequerirCuentaVerificadaUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(ProviderPublicProfileUiState())
    val state: StateFlow<ProviderPublicProfileUiState> = _state.asStateFlow()

    private val _events = Channel<ProviderPublicProfileEvent>()
    val events = _events.receiveAsFlow()

    private var loadJob: Job? = null

    fun onAction(action: ProviderPublicProfileAction) {
        when (action) {
            is ProviderPublicProfileAction.Start -> load(action.providerId)
            ProviderPublicProfileAction.Retry -> _state.value.providerId?.let(::load)
            is ProviderPublicProfileAction.ServiceClicked ->
                emit(ProviderPublicProfileEvent.NavigateToService(action.serviceId))
            is ProviderPublicProfileAction.BookClicked ->
                _state.value.profile?.let { book(action.serviceId, it.provider.id) }
            ProviderPublicProfileAction.BackClicked -> emit(ProviderPublicProfileEvent.NavigateBack)
        }
    }

    private fun emit(event: ProviderPublicProfileEvent) {
        viewModelScope.launch { _events.send(event) }
    }

    /** Same guard as C3's "Reservar": no session -> sign in, unverified email -> verify, else on. */
    private fun book(serviceId: String, providerId: String) {
        viewModelScope.launch {
            _state.update { it.copy(bookingError = null) }
            when (val result = requerirCuentaVerificada()) {
                is DomainResult.Success ->
                    _events.send(ProviderPublicProfileEvent.NavigateToBooking(serviceId, providerId))
                is DomainResult.Failure -> when (val access = result.error.toBookingAccess()) {
                    BookingAccess.SignInRequired -> _events.send(ProviderPublicProfileEvent.SignInRequired)
                    BookingAccess.VerificationRequired -> _events.send(ProviderPublicProfileEvent.VerificationRequired)
                    is BookingAccess.Failed -> _state.update { it.copy(bookingError = access.error) }
                    BookingAccess.Granted -> Unit
                }
            }
        }
    }

    private fun load(providerId: String) {
        loadJob?.cancel()
        _state.value = ProviderPublicProfileUiState(providerId = providerId, isLoading = true)
        loadJob = viewModelScope.launch {
            when (val result = verPerfilProveedor(providerId)) {
                is DomainResult.Success -> _state.update { it.copy(isLoading = false, profile = result.value) }
                is DomainResult.Failure -> _state.update { it.copy(isLoading = false, error = result.error) }
            }
        }
    }
}
