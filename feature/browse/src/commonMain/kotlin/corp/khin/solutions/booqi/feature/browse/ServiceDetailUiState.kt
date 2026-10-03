package corp.khin.solutions.booqi.feature.browse

import corp.khin.solutions.booqi.core.common.DomainError
import corp.khin.solutions.booqi.domain.model.ServiceDetail

/**
 * Immutable state of C3, the Service detail. [serviceId] is the Service this state was loaded
 * *for*: the ViewModel instance is reused across visits, so [ServiceDetailScreen] only trusts the
 * state when it matches the Service it was asked to show (never a flash of the previous one).
 * [isLoading] starts `true` so the first frame is never an empty detail.
 */
data class ServiceDetailUiState(
    val serviceId: String? = null,
    val isLoading: Boolean = true,
    val detail: ServiceDetail? = null,
    val error: DomainError? = null,
)
