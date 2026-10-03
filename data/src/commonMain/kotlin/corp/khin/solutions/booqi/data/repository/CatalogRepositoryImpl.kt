package corp.khin.solutions.booqi.data.repository

import corp.khin.solutions.booqi.core.common.DomainError
import corp.khin.solutions.booqi.core.common.DomainResult
import corp.khin.solutions.booqi.core.common.asFailure
import corp.khin.solutions.booqi.core.common.asSuccess
import corp.khin.solutions.booqi.data.datasource.ProviderProfileRemoteDataSource
import corp.khin.solutions.booqi.data.datasource.ServiceRemoteDataSource
import corp.khin.solutions.booqi.data.mapper.toDomain
import corp.khin.solutions.booqi.domain.model.CatalogEntry
import corp.khin.solutions.booqi.domain.repository.CatalogRepository

/**
 * Joins `Service` to its owning `ProviderProfile` for the Catalog. The join key is the documented
 * contract `Service.providerId == ProviderProfile.id`; a Service with no such profile is dropped —
 * there is **no** fallback join by `ProviderProfile.userId` (today's placeholder ids don't line up,
 * #50 settles that). Visibility rules are the use case's job, not applied here.
 *
 * Caching policy: remote-first, no local cache (same as the other repositories until #9/#27). With
 * Supabase this becomes one `services` + `provider_profiles` join query.
 */
class CatalogRepositoryImpl(
    private val services: ServiceRemoteDataSource,
    private val profiles: ProviderProfileRemoteDataSource,
) : CatalogRepository {

    @Suppress("TooGenericExceptionCaught") // deliberate: repository boundary, see ServiceRepositoryImpl.
    override suspend fun getEntries(): DomainResult<List<CatalogEntry>> = try {
        val all = services.findAll()
        val owners = profiles.findByIds(all.mapTo(mutableSetOf()) { it.providerId }).associateBy { it.id }
        all.mapNotNull { service ->
            owners[service.providerId]?.let { CatalogEntry(service.toDomain(), it.toDomain()) }
        }.asSuccess()
    } catch (e: Exception) {
        DomainError.Unknown(e.message).asFailure()
    }
}
