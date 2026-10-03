package corp.khin.solutions.booqi.feature.provider

/**
 * One-shot effects — delivered via a Channel/Flow, never folded into [ServiceListUiState] (it
 * would replay on every recomposition). Navigation is surfaced as events and turned into the
 * screen's callbacks, the same way `BrowseEvent.NavigateToServiceDetail` is.
 */
sealed interface ServiceListEvent {
    data object NavigateToAddService : ServiceListEvent
    data class NavigateToEditService(val serviceId: String) : ServiceListEvent
    data class ShowError(val message: String) : ServiceListEvent
}
