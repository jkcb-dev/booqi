package corp.khin.solutions.booqi.domain.usecase

import corp.khin.solutions.booqi.core.common.DomainError
import corp.khin.solutions.booqi.core.common.DomainResult
import corp.khin.solutions.booqi.core.common.asFailure
import corp.khin.solutions.booqi.core.common.asSuccess
import corp.khin.solutions.booqi.domain.model.DateRange
import corp.khin.solutions.booqi.domain.model.ProviderProfile
import corp.khin.solutions.booqi.domain.repository.ProviderProfileRepository

/**
 * Hand-written fake shared by the Grupo 1 use case tests (Activar/Completar/Pausar) — a minimal,
 * in-memory implementation of [ProviderProfileRepository] good enough to exercise the BDD
 * scenarios in docs/domain/provider-flow.md § Grupo 1 without any real I/O. Not the same class as
 * `data`'s `FakeProviderProfileRemoteDataSource` — that one fakes the datasource boundary for the
 * app; this one fakes the repository boundary for domain unit tests.
 */
class FakeProviderProfileRepository : ProviderProfileRepository {

    private val profilesById = mutableMapOf<String, ProviderProfile>()
    private var nextId = 1

    override suspend fun activateProviderMode(userId: String): DomainResult<ProviderProfile> {
        val existing = profilesById.values.firstOrNull { it.userId == userId }
        val profile = existing ?: ProviderProfile(id = "profile-${nextId++}", userId = userId)
                .also { profilesById[it.id] = it }
        return profile.asSuccess()
    }

    override suspend fun completeProfile(
        profileId: String,
        name: String,
        photoUrl: String,
        description: String,
        location: String,
    ): DomainResult<ProviderProfile> {
        val existing = profilesById[profileId] ?: return DomainError.NotFound.asFailure()
        val updated = existing.copy(
            name = name,
            photoUrl = photoUrl,
            description = description,
            location = location,
            isComplete = true,
        )
        profilesById[profileId] = updated
        return updated.asSuccess()
    }

    override suspend fun setPausedRange(profileId: String, pausedRange: DateRange?): DomainResult<ProviderProfile> {
        val existing = profilesById[profileId] ?: return DomainError.NotFound.asFailure()
        val updated = existing.copy(pausedRange = pausedRange)
        profilesById[profileId] = updated
        return updated.asSuccess()
    }

    override suspend fun getProfile(profileId: String): DomainResult<ProviderProfile> =
        profilesById[profileId]?.asSuccess() ?: DomainError.NotFound.asFailure()

    override suspend fun updateRating(
        profileId: String,
        ratingAverage: Double?,
        ratingCount: Int,
    ): DomainResult<ProviderProfile> {
        val existing = profilesById[profileId] ?: return DomainError.NotFound.asFailure()
        val updated = existing.copy(ratingAverage = ratingAverage, ratingCount = ratingCount)
        profilesById[profileId] = updated
        return updated.asSuccess()
    }

    override suspend fun findByUserId(userId: String): DomainResult<ProviderProfile?> =
        profilesById.values.firstOrNull { it.userId == userId }.asSuccess()

    override suspend fun anonymizeProfile(profileId: String): DomainResult<ProviderProfile> {
        val existing = profilesById[profileId] ?: return DomainError.NotFound.asFailure()
        val updated = existing.copy(
            name = null,
            photoUrl = null,
            description = null,
            location = null,
            coordinates = null,
            pausedRange = null,
            isComplete = false,
        )
        profilesById[profileId] = updated
        return updated.asSuccess()
    }
}
