package corp.khin.solutions.booqi.domain.repository

import corp.khin.solutions.booqi.core.common.DomainResult
import corp.khin.solutions.booqi.domain.model.ServiceProvider

/**
 * Domain-owned contract. The implementation (in `data`) decides how featured providers are
 * sourced/cached — this interface is the only thing a use case is allowed to know about.
 */
@Deprecated(
    "Serves the pre-correction ServiceProvider model. Replaced by CatalogRepository; remove once " +
        "feature:browse is migrated (#21).",
)
interface ServiceCatalogRepository {
    suspend fun getFeaturedProviders(): DomainResult<List<ServiceProvider>>
}
