package corp.khin.solutions.booqi.domain.usecase

import corp.khin.solutions.booqi.core.common.DomainResult
import corp.khin.solutions.booqi.domain.model.ServiceProvider
import corp.khin.solutions.booqi.domain.repository.ServiceCatalogRepository

/**
 * One class, one action (SRP). Presentation calls this, never [ServiceCatalogRepository]
 * directly — that indirection is where future business rules (e.g. "hide providers below a
 * rating threshold") land without touching the ViewModel.
 */
@Deprecated(
    "Serves the pre-correction ServiceProvider model. Use BuscarServiciosUseCase (search Services) and " +
        "VerPerfilProveedorUseCase; remove once feature:browse is migrated (#21).",
)
class GetFeaturedProvidersUseCase(
    private val repository: ServiceCatalogRepository,
) {
    suspend operator fun invoke(): DomainResult<List<ServiceProvider>> = repository.getFeaturedProviders()
}
