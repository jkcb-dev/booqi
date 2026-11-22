package corp.khin.solutions.booqi.feature.booking

import corp.khin.solutions.booqi.core.common.DomainError
import corp.khin.solutions.booqi.domain.model.Address
import corp.khin.solutions.booqi.domain.usecase.BuscarDireccionUseCase

/**
 * Immutable state of the address picker (C6). [savedAddress] is the Customer's single saved address
 * (`null` = none yet, so saving is an add; otherwise an update); [isLoading]/[loadError] belong to
 * reading it on entry. The search side is [query] -> [results] ([isSearching], [searchError]);
 * [selected] is the candidate chosen to be saved and [saveError] the form error of the last save
 * (`InvalidInput` renders on the form, never as a crash).
 */
data class AddressUiState(
    val isLoading: Boolean = true,
    val loadError: DomainError? = null,
    val savedAddress: Address? = null,
    val query: String = "",
    val isSearching: Boolean = false,
    val results: List<Address> = emptyList(),
    val searchError: DomainError? = null,
    val selected: Address? = null,
    val isSaving: Boolean = false,
    val saveError: String? = null,
) {
    /** True once the query is long enough to have been searched and the answer is in. */
    val hasSearched: Boolean
        get() = query.trim().length >= BuscarDireccionUseCase.MIN_QUERY_LENGTH && !isSearching && searchError == null

    val canSave: Boolean get() = selected != null && !isSaving
}
