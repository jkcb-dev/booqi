package corp.khin.solutions.booqi.feature.provider

/** One-shot effects of the calendar — never folded into [DateBlockingUiState]. */
sealed interface DateBlockingEvent {
    data class ShowError(val message: String) : DateBlockingEvent
}
