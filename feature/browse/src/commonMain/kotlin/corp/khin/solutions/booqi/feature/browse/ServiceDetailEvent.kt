package corp.khin.solutions.booqi.feature.browse

/** One-shot effects of C3 — never folded into [ServiceDetailUiState] (they would replay). */
sealed interface ServiceDetailEvent {
    data class NavigateToProvider(val providerId: String) : ServiceDetailEvent
    data class NavigateToBooking(val serviceId: String, val providerId: String) : ServiceDetailEvent

    /** "Reservar" without a session: the visitor signs in first and comes back here. */
    data object SignInRequired : ServiceDetailEvent

    /** "Reservar" with an unverified email: the verification notice first. */
    data object VerificationRequired : ServiceDetailEvent

    data object NavigateBack : ServiceDetailEvent
}
