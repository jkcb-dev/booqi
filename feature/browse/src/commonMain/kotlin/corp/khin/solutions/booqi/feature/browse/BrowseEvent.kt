package corp.khin.solutions.booqi.feature.browse

/**
 * One-shot effects — delivered via a Channel/Flow, never folded into [BrowseUiState]. If these
 * lived in the state instead, they'd replay on every recomposition/process restore (e.g. the
 * navigation would refire). Search failures are not events: they stay in the state and render
 * inline with a retry.
 */
sealed interface BrowseEvent {
    data class NavigateToServiceDetail(val serviceId: String) : BrowseEvent
}
