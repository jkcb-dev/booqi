package corp.khin.solutions.booqi.feature.browse

import corp.khin.solutions.booqi.domain.model.ServiceCategory

/** User intents. The Composable only ever calls [BrowseViewModel.onAction] with one of these. */
sealed interface BrowseAction {
    /**
     * Sent every time the screen enters composition (and by the error "Reintentar"): re-runs the
     * current search so coming back from a detail never shows a stale list. Does nothing before the
     * first search.
     */
    data object Refresh : BrowseAction

    /** The text field changed. Searching waits for [SearchSubmitted]. */
    data class QueryChanged(val text: String) : BrowseAction

    /** The Customer submitted the text (keyboard "search" or the Buscar button). */
    data object SearchSubmitted : BrowseAction

    /** A category chip; `null` is "Todos". Searches straight away, keeping the text and distance. */
    data class CategorySelected(val category: ServiceCategory?) : BrowseAction

    /** A distance chip, in km; `null` is "Cualquiera". */
    data class DistanceSelected(val km: Int?) : BrowseAction

    /** A "Recientes" entry: searches for it again. */
    data class RecentSelected(val text: String) : BrowseAction

    /** Back to C1: empty text, "Todos", no distance filter. [BrowseUiState.recentSearches] stay. */
    data object ClearSearch : BrowseAction

    data class ServiceSelected(val serviceId: String) : BrowseAction
}
