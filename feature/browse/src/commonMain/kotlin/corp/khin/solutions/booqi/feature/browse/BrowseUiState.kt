package corp.khin.solutions.booqi.feature.browse

import corp.khin.solutions.booqi.core.common.DomainError
import corp.khin.solutions.booqi.domain.model.ServiceProvider

/** Immutable, single source of truth for the Browse screen. Survives recomposition as-is. */
data class BrowseUiState(
    val isLoading: Boolean = true,
    val providers: List<ServiceProvider> = emptyList(),
    val error: DomainError? = null,
)
