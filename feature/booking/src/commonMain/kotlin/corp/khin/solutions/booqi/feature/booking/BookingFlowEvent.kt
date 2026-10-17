package corp.khin.solutions.booqi.feature.booking

/** One-shot effects of the request flow — never folded into [BookingFlowUiState] (they would replay). */
sealed interface BookingFlowEvent {
    /** The Identity guard refused: no session, or an unverified email. */
    data object SignInRequired : BookingFlowEvent

    /** A Domicilio request has no saved address: the Customer must add one. */
    data object AddAddress : BookingFlowEvent

    /** The request was sent; [bookingId] is the new pending Booking (C8). */
    data class Done(val bookingId: String) : BookingFlowEvent

    /** The Customer backed out of the flow. */
    data object Finished : BookingFlowEvent
}
