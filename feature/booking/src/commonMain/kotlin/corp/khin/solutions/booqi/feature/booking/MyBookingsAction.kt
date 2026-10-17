package corp.khin.solutions.booqi.feature.booking

/** User intents on C11. The Composable only ever calls [MyBookingsViewModel.onAction]. */
sealed interface MyBookingsAction {
    /** Sent on every entry: resets to "Todas" and reloads, so a status changed elsewhere shows. */
    data object Start : MyBookingsAction
    data object Retry : MyBookingsAction
    data class SelectTab(val tab: BookingsTab) : MyBookingsAction
    data class BookingClicked(val bookingId: String) : MyBookingsAction
    data object SignInClicked : MyBookingsAction

    /** The empty state's CTA: go find a Service. */
    data object ExploreClicked : MyBookingsAction
}
