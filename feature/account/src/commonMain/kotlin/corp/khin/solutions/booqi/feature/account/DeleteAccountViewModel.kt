package corp.khin.solutions.booqi.feature.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import corp.khin.solutions.booqi.core.common.DomainResult
import corp.khin.solutions.booqi.domain.model.AccountDeletionResult
import corp.khin.solutions.booqi.domain.model.ActiveBookings
import corp.khin.solutions.booqi.domain.usecase.EliminarCuentaUseCase
import corp.khin.solutions.booqi.domain.usecase.ObtenerServicioUseCase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Account deletion (docs/domain/identity-flow.md — `EliminarCuenta`), hosted by the profile screen.
 * Talks to the domain through [EliminarCuentaUseCase] only (plus [ObtenerServicioUseCase] to name
 * the blocking Bookings' Services). The use case either deletes the account or refuses with the
 * active Bookings that must be cancelled or completed first — there is no dry run, so the flow is
 * confirmation first, then the outcome: the session ends, or the blocking list is shown.
 *
 * Same reuse rule as [SignInViewModel]: [DeleteAccountSection] sends [DeleteAccountAction.Start]
 * on every entry.
 */
class DeleteAccountViewModel(
    private val eliminarCuenta: EliminarCuentaUseCase,
    private val obtenerServicio: ObtenerServicioUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(DeleteAccountUiState())
    val state: StateFlow<DeleteAccountUiState> = _state.asStateFlow()

    private val _events = Channel<DeleteAccountEvent>()
    val events = _events.receiveAsFlow()

    fun onAction(action: DeleteAccountAction) {
        when (action) {
            DeleteAccountAction.Start -> _state.value = DeleteAccountUiState()
            DeleteAccountAction.DeleteClicked -> _state.update { it.copy(isConfirming = true, error = null) }
            DeleteAccountAction.ConfirmClicked -> delete()
            DeleteAccountAction.DismissClicked ->
                if (!_state.value.isDeleting) _state.value = DeleteAccountUiState()
        }
    }

    /** Escenarios: "El Usuario elimina su cuenta sin reservas activas" / "...intenta eliminar su
     * cuenta con reservas activas". A refusal changes nothing and is not an error. */
    private fun delete() {
        if (_state.value.isDeleting) return
        _state.update { it.copy(isDeleting = true, error = null) }
        viewModelScope.launch {
            when (val result = eliminarCuenta()) {
                is DomainResult.Success -> when (val outcome = result.value) {
                    AccountDeletionResult.Deleted -> {
                        _state.value = DeleteAccountUiState()
                        _events.send(DeleteAccountEvent.AccountDeleted)
                    }
                    is AccountDeletionResult.Blocked -> showBlocked(outcome.activeBookings)
                }
                is DomainResult.Failure -> _state.update { it.copy(isDeleting = false, error = result.error) }
            }
        }
    }

    private suspend fun showBlocked(active: ActiveBookings) {
        val titles = (active.asCustomer + active.asProvider).map { it.serviceId }.distinct()
            .mapNotNull { id -> (obtenerServicio(id) as? DomainResult.Success)?.let { id to it.value.title } }
            .toMap()
        _state.update {
            it.copy(isConfirming = false, isDeleting = false, blocked = active, serviceTitles = titles)
        }
    }
}
