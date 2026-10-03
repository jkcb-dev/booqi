package corp.khin.solutions.booqi.data.datasource

import corp.khin.solutions.booqi.data.dto.ProviderProfileDto

/**
 * TEMPORARY. In-memory stand-in for a real Supabase-backed [ProviderProfileRemoteDataSource]
 * until #27 (Supabase schema + `supabase-kt` wiring) lands — see `docs/DATABASE.md`. Mirrors the
 * pattern in [FakeProviderRemoteDataSource]: this exists purely so the module graph and MVI
 * wiring can be proven end-to-end without blocking on backend implementation work. Replace, don't
 * extend.
 *
 * Not thread-safe by design — a single fake, single-process instance has no concurrent-writer
 * scenario worth guarding against; a real datasource will get that from the backend instead.
 */
class FakeProviderProfileRemoteDataSource(
    seed: List<ProviderProfileDto> = emptyList(),
) : ProviderProfileRemoteDataSource {

    private val profilesById = mutableMapOf<String, ProviderProfileDto>().apply {
        seed.forEach { put(it.id, it) }
    }
    private var nextId = 1

    override suspend fun createEmptyProfile(userId: String): ProviderProfileDto {
        val profile = ProviderProfileDto(
            id = "provider-profile-${nextId++}",
            userId = userId,
            name = null,
            photoUrl = null,
            description = null,
            location = null,
            isComplete = false,
            pausedRangeStart = null,
            pausedRangeEnd = null,
            ratingAverage = null,
            ratingCount = 0,
            locationLat = null,
            locationLng = null,
        )
        profilesById[profile.id] = profile
        return profile
    }

    override suspend fun findByUserId(userId: String): ProviderProfileDto? =
        profilesById.values.firstOrNull { it.userId == userId }

    override suspend fun findById(profileId: String): ProviderProfileDto? = profilesById[profileId]

    override suspend fun findByIds(ids: Set<String>): List<ProviderProfileDto> =
        ids.mapNotNull { profilesById[it] }

    override suspend fun save(profile: ProviderProfileDto): ProviderProfileDto {
        profilesById[profile.id] = profile
        return profile
    }
}
