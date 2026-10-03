package corp.khin.solutions.booqi.feature.browse

import corp.khin.solutions.booqi.core.common.DomainError
import corp.khin.solutions.booqi.domain.model.ProviderPublicProfile

/**
 * Immutable state of C4, a Provider's public page. [providerId] is the Provider this state was
 * loaded *for* (the ViewModel instance is reused across visits, see [ServiceDetailUiState]).
 */
data class ProviderPublicProfileUiState(
    val providerId: String? = null,
    val isLoading: Boolean = true,
    val profile: ProviderPublicProfile? = null,
    val error: DomainError? = null,
)
