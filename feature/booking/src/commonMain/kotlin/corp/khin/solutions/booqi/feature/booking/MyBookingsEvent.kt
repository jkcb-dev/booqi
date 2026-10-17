package corp.khin.solutions.booqi.feature.booking

/** One-shot effects of C11 — never folded into [MyBookingsUiState] (they would replay). */
sealed interface MyBookingsEvent {
    data class BookingSelected(val bookingId: String) : MyBookingsEvent
    data object SignInRequired : MyBookingsEvent
    data object Explore : MyBookingsEvent
}
