package corp.khin.solutions.booqi.feature.booking

import corp.khin.solutions.booqi.core.common.DomainError
import corp.khin.solutions.booqi.domain.model.Booking
import corp.khin.solutions.booqi.domain.model.BookingHistory
import corp.khin.solutions.booqi.domain.model.BookingStatus
import kotlin.time.Instant

/**
 * C11's tabs: [ALL] (every status, in the history's fixed order) and one per status the Customer
 * most wants to find. The closed statuses (rejected, expired, cancelled) live under [ALL].
 */
enum class BookingsTab(val label: String, val status: BookingStatus?) {
    ALL("Todas", null),
    CONFIRMED("Confirmada", BookingStatus.CONFIRMED),
    REQUESTED("Pendiente", BookingStatus.REQUESTED),
    COMPLETED("Completada", BookingStatus.COMPLETED),
}

/**
 * Immutable state of C11 "Mis reservas". [needsSignIn] is the visitor case (`Unauthorized`): the
 * history needs a session. [titles] resolves `Booking.serviceId` to a title (the Booking only
 * holds the id); a missing entry shows a generic label. [now] decides which pending requests have
 * lapsed ([effectiveStatus]) so a stale "Pendiente" is shown as expired.
 */
data class MyBookingsUiState(
    val now: Instant,
    val tab: BookingsTab = BookingsTab.ALL,
    val isLoading: Boolean = true,
    val history: BookingHistory? = null,
    val titles: Map<String, String> = emptyMap(),
    val error: DomainError? = null,
    val needsSignIn: Boolean = false,
) {
    /** Every Booking in the history's order (empty until loaded). */
    val allBookings: List<Booking> get() = history?.bookings.orEmpty()

    /** What the selected tab lists. */
    val visibleBookings: List<Booking>
        get() = tab.status?.let { wanted -> allBookings.filter { it.effectiveStatus(now) == wanted } } ?: allBookings

    /** True once loaded and the Customer has no Booking at all (an empty list, not an error). */
    val hasNoBookings: Boolean get() = !isLoading && error == null && !needsSignIn && allBookings.isEmpty()
}
