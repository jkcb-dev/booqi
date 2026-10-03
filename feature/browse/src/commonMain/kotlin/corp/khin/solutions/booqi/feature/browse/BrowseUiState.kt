package corp.khin.solutions.booqi.feature.browse

import corp.khin.solutions.booqi.core.common.DomainError
import corp.khin.solutions.booqi.domain.model.ServiceCategory
import corp.khin.solutions.booqi.domain.model.ServiceSearchResult

/**
 * Immutable state of the Customer's search (docs/design/SCREENS.md C1 + C2 on one screen).
 *
 * [hasSearched] tells the two halves apart: `false` is C1 (input, chips and the "Recientes"
 * section), `true` is C2 (the same input and chips over the distance filter, the result counter and
 * the list). [category] `null` is the "Todos" chip; [radiusKm] `null` is no distance filter.
 * [recentSearches] are the submitted texts, newest first, kept for the app session only.
 */
data class BrowseUiState(
    val query: String = "",
    val category: ServiceCategory? = null,
    val radiusKm: Int? = null,
    val hasSearched: Boolean = false,
    val isLoading: Boolean = false,
    val results: List<ServiceSearchResult> = emptyList(),
    val recentSearches: List<String> = emptyList(),
    val error: DomainError? = null,
) {
    /** The counter shown on C2. */
    val resultCount: Int get() = results.size
}
