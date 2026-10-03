package corp.khin.solutions.booqi.feature.provider

import corp.khin.solutions.booqi.core.common.DomainError
import corp.khin.solutions.booqi.domain.model.Service

/**
 * Immutable state of the Provider's service list (docs/design/SCREENS.md P4). [services] holds
 * **all** of the Provider's services in creation order, disabled ones included
 * (docs/domain/provider-flow.md § Grupo 2).
 */
data class ServiceListUiState(
    val isLoading: Boolean = false,
    val services: List<Service> = emptyList(),
    /** Ids whose enable/disable toggle is in flight — the Switch is disabled meanwhile. */
    val togglingServiceIds: Set<String> = emptySet(),
    val error: DomainError? = null,
)
