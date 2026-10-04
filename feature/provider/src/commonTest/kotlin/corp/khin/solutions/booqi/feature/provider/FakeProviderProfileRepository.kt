package corp.khin.solutions.booqi.feature.provider

import corp.khin.solutions.booqi.core.common.DomainError
import corp.khin.solutions.booqi.core.common.DomainResult
import corp.khin.solutions.booqi.core.common.asFailure
import corp.khin.solutions.booqi.core.common.asSuccess
import corp.khin.solutions.booqi.domain.model.DateRange
import corp.khin.solutions.booqi.domain.model.ProviderProfile
import corp.khin.solutions.booqi.domain.repository.ProviderProfileRepository

/**
 * Minimal in-memory fake for [ProviderProfileRepository], scoped to
 * [ProviderProfileViewModel]'s reducer tests. Deliberately not the same class as `domain`'s own
 * `FakeProviderProfileRepository` (commonTest sourceSets aren't shared across Gradle modules) —
 * same shape, kept in sync by hand since both exist to satisfy the same Grupo 1 Gherkin
 * scenarios, just at different layers (use case vs. ViewModel).
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

    // Added by #18 to ProviderProfileRepository (Booking/rating side); not exercised by the
    // profile ViewModel tests but required for the fake to compile.
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

    // Added by #56 (Identity) to ProviderProfileRepository; not exercised by the profile ViewModel
    // tests but required for the fake to compile.
    override suspend fun findByUserId(userId: String): DomainResult<ProviderProfile?> =
        profilesById.values.firstOrNull { it.userId == userId }.asSuccess()

    override suspend fun anonymizeProfile(profileId: String): DomainResult<ProviderProfile> =
        DomainError.Unknown("not used by feature:provider").asFailure()
}
