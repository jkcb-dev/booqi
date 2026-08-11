package corp.khin.solutions.booqi.data.repository

import corp.khin.solutions.booqi.core.common.DomainError
import corp.khin.solutions.booqi.core.common.DomainResult
import corp.khin.solutions.booqi.core.common.asFailure
import corp.khin.solutions.booqi.core.common.asSuccess
import corp.khin.solutions.booqi.data.datasource.ProviderRemoteDataSource
import corp.khin.solutions.booqi.data.mapper.toDomain
import corp.khin.solutions.booqi.domain.model.ServiceProvider
import corp.khin.solutions.booqi.domain.repository.ServiceCatalogRepository

/**
 * Caching policy for this repository: remote-first, no local cache yet. Featured-provider
 * availability goes stale fast, so serving a local copy while the network is reachable would be
 * actively wrong. Once `core:database` gets a real schema, revisit this for an offline fallback
 * (serve last-known-good on [DomainError.NoConnection] instead of failing outright) — that's a
 * deliberate later decision, not an oversight.
 */
class ServiceCatalogRepositoryImpl(
    private val remoteDataSource: ProviderRemoteDataSource,
) : ServiceCatalogRepository {

    @Suppress("TooGenericExceptionCaught") // deliberate: this boundary is where every real
    // exception (network, serialization, ...) gets translated into a DomainError — see
    // core:common's DomainResult docs. Narrowing this catch would just leak raw exceptions
    // into presentation instead of preventing them.
    override suspend fun getFeaturedProviders(): DomainResult<List<ServiceProvider>> = try {
        remoteDataSource.fetchFeaturedProviders().map { it.toDomain() }.asSuccess()
    } catch (e: Exception) {
        DomainError.Unknown(e.message).asFailure()
    }
}
