package corp.khin.solutions.booqi.feature.browse

/**
 * One-shot effects — delivered via a Channel/Flow, never folded into [BrowseUiState]. If these
 * lived in the state instead, they'd replay on every recomposition/process restore (e.g. the
 * navigation would refire).
 */
sealed interface BrowseEvent {
    data class NavigateToProviderDetail(val providerId: String) : BrowseEvent
    data class ShowError(val message: String) : BrowseEvent
}
