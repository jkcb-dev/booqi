package corp.khin.solutions.booqi.data.datasource

import corp.khin.solutions.booqi.data.dto.ProviderProfileDto

/**
 * Source of `ProviderProfile` data — create-on-activate, then read/mutate by id. `data`-only
 * concern; the DTO shape never crosses into `domain`
 * ([corp.khin.solutions.booqi.data.mapper] does that translation).
 */
interface ProviderProfileRemoteDataSource {

    /** Creates and persists a brand-new, empty profile for [userId]. Id assignment is this
     * datasource's responsibility (mirroring how a real backend would assign one on insert). */
    suspend fun createEmptyProfile(userId: String): ProviderProfileDto

    suspend fun findByUserId(userId: String): ProviderProfileDto?

    suspend fun findById(profileId: String): ProviderProfileDto?

    /** Upserts by [ProviderProfileDto.id]. */
    suspend fun save(profile: ProviderProfileDto): ProviderProfileDto
}
