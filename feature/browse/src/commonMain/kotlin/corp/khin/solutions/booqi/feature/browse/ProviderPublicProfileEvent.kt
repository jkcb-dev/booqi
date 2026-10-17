package corp.khin.solutions.booqi.feature.browse

/** One-shot effects of C4 — never folded into [ProviderPublicProfileUiState] (they would replay). */
sealed interface ProviderPublicProfileEvent {
    data class NavigateToService(val serviceId: String) : ProviderPublicProfileEvent
    data class NavigateToBooking(val serviceId: String, val providerId: String) : ProviderPublicProfileEvent

    /** "Reservar ›" without a session: the visitor signs in first and comes back here. */
    data object SignInRequired : ProviderPublicProfileEvent

    /** "Reservar ›" with an unverified email: the verification notice first. */
    data object VerificationRequired : ProviderPublicProfileEvent

    data object NavigateBack : ProviderPublicProfileEvent
}
