package corp.khin.solutions.booqi.feature.provider

/**
 * One-shot effects — delivered via a Channel/Flow, never folded into [BookingInboxUiState] (it
 * would replay on every recomposition). Errors are not events: a failed action shows inline in
 * the detail, a failed load shows the retry state.
 */
sealed interface BookingInboxEvent {
    /** The Provider pressed "Volver" on the lists: wiring maps this to a pop. */
    data object Finished : BookingInboxEvent
}
