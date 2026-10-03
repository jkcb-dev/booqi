package corp.khin.solutions.booqi.feature.browse

/** User intents on C4. The Composable only ever calls [ProviderPublicProfileViewModel.onAction]. */
sealed interface ProviderPublicProfileAction {
    /** Sent on every entry for [providerId]: resets the screen and loads that Provider afresh. */
    data class Start(val providerId: String) : ProviderPublicProfileAction

    data object Retry : ProviderPublicProfileAction

    /** A Service card was tapped: opens its C3 detail. */
    data class ServiceClicked(val serviceId: String) : ProviderPublicProfileAction

    /** A card's "Reservar ›" — handed to the booking flow (#26) by a callback. */
    data class BookClicked(val serviceId: String) : ProviderPublicProfileAction

    data object BackClicked : ProviderPublicProfileAction
}
