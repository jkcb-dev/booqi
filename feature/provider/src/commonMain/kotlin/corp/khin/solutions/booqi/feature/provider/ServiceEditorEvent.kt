package corp.khin.solutions.booqi.feature.provider

/** One-shot effects of the service form — never folded into [ServiceEditorUiState]. */
sealed interface ServiceEditorEvent {
    /** The service was added/edited; the screen calls its `onFinished` callback. */
    data object Saved : ServiceEditorEvent
    data class ShowError(val message: String) : ServiceEditorEvent
}
