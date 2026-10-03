package corp.khin.solutions.booqi.data.datasource

import corp.khin.solutions.booqi.data.dto.ServiceDetailsDto
import corp.khin.solutions.booqi.data.dto.ServiceDto

/**
 * TEMPORARY. In-memory stand-in for a real Supabase-backed [ServiceRemoteDataSource] until #27
 * (Supabase schema + `supabase-kt` wiring) lands — see `docs/DATABASE.md`. Mirrors the pattern in
 * [FakeProviderProfileRemoteDataSource]: this exists purely so the module graph and MVI wiring
 * can be proven end-to-end without blocking on backend implementation work. Replace, don't extend.
 *
 * Not thread-safe by design — a single fake, single-process instance has no concurrent-writer
 * scenario worth guarding against; a real datasource will get that from the backend instead.
 */
class FakeServiceRemoteDataSource(
    seed: List<ServiceDto> = emptyList(),
) : ServiceRemoteDataSource {

    private val servicesById = mutableMapOf<String, ServiceDto>().apply {
        seed.forEach { put(it.id, it) }
    }
    private var nextId = 1

    override suspend fun create(providerId: String, details: ServiceDetailsDto): ServiceDto {
        val service = ServiceDto(
            id = "service-${nextId++}",
            providerId = providerId,
            title = details.title,
            photoUrl = details.photoUrl,
            description = details.description,
            priceCents = details.priceCents,
            durationMinutes = details.durationMinutes,
            modality = details.modality,
            isActive = true,
            category = details.category ?: DEFAULT_CATEGORY,
        )
        servicesById[service.id] = service
        return service
    }

    override suspend fun findById(serviceId: String): ServiceDto? = servicesById[serviceId]

    // servicesById is a LinkedHashMap (mutableMapOf), so values iterate in insertion order, and
    // save() on an existing key keeps its position — i.e. creation order, stable across edits.
    override suspend fun findByProviderId(providerId: String): List<ServiceDto> =
        servicesById.values.filter { it.providerId == providerId }

    override suspend fun findAll(): List<ServiceDto> = servicesById.values.toList()

    override suspend fun save(service: ServiceDto): ServiceDto {
        servicesById[service.id] = service
        return service
    }

    private companion object {
        const val DEFAULT_CATEGORY = "otro"
    }
}
