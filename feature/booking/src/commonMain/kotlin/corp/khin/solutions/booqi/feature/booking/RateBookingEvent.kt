package corp.khin.solutions.booqi.feature.booking

/** One-shot effects of C12 — never folded into [RateBookingUiState] (they would replay). */
sealed interface RateBookingEvent {
    /** The rating was saved, or the Customer backed out: leave the screen. */
    data object Finished : RateBookingEvent
}
