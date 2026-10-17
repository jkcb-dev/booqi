package corp.khin.solutions.booqi.feature.booking

/** User intents on C12. The Composable only ever calls [RateBookingViewModel.onAction]. */
sealed interface RateBookingAction {
    /** Sent on every entry for [bookingId]: resets the form and loads that Booking afresh. */
    data class Start(val bookingId: String) : RateBookingAction
    data object Retry : RateBookingAction
    data class StarsSelected(val stars: Int) : RateBookingAction
    data class CommentChanged(val value: String) : RateBookingAction
    data object Submit : RateBookingAction
    data object BackClicked : RateBookingAction
}
