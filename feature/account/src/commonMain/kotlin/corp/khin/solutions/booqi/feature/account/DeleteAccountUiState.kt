package corp.khin.solutions.booqi.feature.account

import corp.khin.solutions.booqi.core.common.DomainError
import corp.khin.solutions.booqi.domain.model.ActiveBookings

/**
 * Immutable state of the account-deletion flow, a dialog over the profile. Closed unless
 * [isConfirming] (the "¿Eliminar tu cuenta?" confirmation) or [blocked] is set. [blocked] is the
 * refusal: the active Bookings, as Cliente and as Proveedor, that must be cancelled or completed
 * first; [serviceTitles] maps their `serviceId` to a title (a Booking only references its Service;
 * a missing entry renders as a generic "Servicio").
 */
data class DeleteAccountUiState(
    val isConfirming: Boolean = false,
    val isDeleting: Boolean = false,
    val blocked: ActiveBookings? = null,
    val serviceTitles: Map<String, String> = emptyMap(),
    val error: DomainError? = null,
) {
    val isOpen: Boolean get() = isConfirming || blocked != null
}
