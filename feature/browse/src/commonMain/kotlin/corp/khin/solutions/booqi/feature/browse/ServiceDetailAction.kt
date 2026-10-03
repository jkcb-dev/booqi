package corp.khin.solutions.booqi.feature.browse

/** User intents on C3. The Composable only ever calls [ServiceDetailViewModel.onAction]. */
sealed interface ServiceDetailAction {
    /** Sent on every entry for [serviceId]: resets the screen and loads that Service afresh. */
    data class Start(val serviceId: String) : ServiceDetailAction

    data object Retry : ServiceDetailAction

    /** The Provider block was tapped: opens C4. */
    data object ProviderClicked : ServiceDetailAction

    /** "Reservar" — handed to the booking flow (#26) by a callback; nothing is built here. */
    data object BookClicked : ServiceDetailAction

    data object BackClicked : ServiceDetailAction
}
