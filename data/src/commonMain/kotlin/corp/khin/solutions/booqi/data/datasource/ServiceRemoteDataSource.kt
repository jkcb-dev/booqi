package corp.khin.solutions.booqi.data.datasource

import corp.khin.solutions.booqi.data.dto.ServiceDetailsDto
import corp.khin.solutions.booqi.data.dto.ServiceDto

/**
 * Source of `Service` data — create, then read/mutate by id. `data`-only concern; the DTO shape
 * never crosses into `domain` ([corp.khin.solutions.booqi.data.mapper] does that translation).
 */
interface ServiceRemoteDataSource {

    /** Creates and persists a brand-new, active [ServiceDto] owned by [providerId]. Id assignment
     * is this datasource's responsibility (mirroring how a real backend would assign one on
     * insert) — mirrors [ProviderProfileRemoteDataSource.createEmptyProfile]'s shape. */
    suspend fun create(providerId: String, details: ServiceDetailsDto): ServiceDto

    suspend fun findById(serviceId: String): ServiceDto?

    /** Every service owned by [providerId], active or not, oldest first (creation order). Empty
     * if the provider has none. */
    suspend fun findByProviderId(providerId: String): List<ServiceDto>

    /** Every service of every provider, active or not, in creation order — the Catalog's source
     * (it applies the visibility rules; see [corp.khin.solutions.booqi.domain.repository.CatalogRepository]). */
    suspend fun findAll(): List<ServiceDto>

    /** Upserts by [ServiceDto.id]. */
    suspend fun save(service: ServiceDto): ServiceDto
}
