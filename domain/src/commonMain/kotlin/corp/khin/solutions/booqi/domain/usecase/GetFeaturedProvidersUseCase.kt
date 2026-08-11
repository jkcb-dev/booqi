package corp.khin.solutions.booqi.domain.usecase

import corp.khin.solutions.booqi.core.common.DomainResult
import corp.khin.solutions.booqi.domain.model.ServiceProvider
import corp.khin.solutions.booqi.domain.repository.ServiceCatalogRepository

/**
 * One class, one action (SRP). Presentation calls this, never [ServiceCatalogRepository]
 * directly — that indirection is where future business rules (e.g. "hide providers below a
 * rating threshold") land without touching the ViewModel.
 */
class GetFeaturedProvidersUseCase(
    private val repository: ServiceCatalogRepository,
) {
    suspend operator fun invoke(): DomainResult<List<ServiceProvider>> = repository.getFeaturedProviders()
}
