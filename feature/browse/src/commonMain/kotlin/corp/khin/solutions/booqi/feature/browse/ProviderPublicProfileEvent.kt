package corp.khin.solutions.booqi.feature.browse

/** One-shot effects of C4 — never folded into [ProviderPublicProfileUiState] (they would replay). */
sealed interface ProviderPublicProfileEvent {
    data class NavigateToService(val serviceId: String) : ProviderPublicProfileEvent
    data class NavigateToBooking(val serviceId: String, val providerId: String) : ProviderPublicProfileEvent
    data object NavigateBack : ProviderPublicProfileEvent
}
