package corp.khin.solutions.booqi.data.repository

import corp.khin.solutions.booqi.core.common.DomainError
import corp.khin.solutions.booqi.core.common.DomainResult
import corp.khin.solutions.booqi.core.common.asFailure
import corp.khin.solutions.booqi.core.common.asSuccess
import corp.khin.solutions.booqi.data.datasource.ProviderProfileRemoteDataSource
import corp.khin.solutions.booqi.data.mapper.toDomain
import corp.khin.solutions.booqi.data.mapper.toDto
import corp.khin.solutions.booqi.domain.model.DateRange
import corp.khin.solutions.booqi.domain.model.ProviderProfile
import corp.khin.solutions.booqi.domain.repository.ProviderProfileRepository

class ProviderProfileRepositoryImpl(
    private val remoteDataSource: ProviderProfileRemoteDataSource,
) : ProviderProfileRepository {

    @Suppress("TooGenericExceptionCaught") // deliberate: this boundary is where every real
    // exception (network, serialization, ...) gets translated into a DomainError — see
    // core:common's DomainResult docs. Narrowing this catch would just leak raw exceptions into
    // domain/presentation instead of preventing them. Same pattern as the other repository implementations.
    override suspend fun activateProviderMode(userId: String): DomainResult<ProviderProfile> = try {
        val profile = remoteDataSource.findByUserId(userId)
            ?: remoteDataSource.createEmptyProfile(userId)
        profile.toDomain().asSuccess()
    } catch (e: Exception) {
        DomainError.Unknown(e.message).asFailure()
    }

    @Suppress("TooGenericExceptionCaught")
    override suspend fun completeProfile(
        profileId: String,
        name: String,
        photoUrl: String,
        description: String,
        location: String,
    ): DomainResult<ProviderProfile> = try {
        val existing = remoteDataSource.findById(profileId)
            ?: return DomainError.NotFound.asFailure()
        val updated = existing.toDomain().copy(
            name = name,
            photoUrl = photoUrl,
            description = description,
            location = location,
            isComplete = true,
        )
        remoteDataSource.save(updated.toDto()).toDomain().asSuccess()
    } catch (e: Exception) {
        DomainError.Unknown(e.message).asFailure()
    }

    @Suppress("TooGenericExceptionCaught")
    override suspend fun setPausedRange(
        profileId: String,
        pausedRange: DateRange?,
    ): DomainResult<ProviderProfile> = try {
        val existing = remoteDataSource.findById(profileId)
            ?: return DomainError.NotFound.asFailure()
        val updated = existing.toDomain().copy(pausedRange = pausedRange)
        remoteDataSource.save(updated.toDto()).toDomain().asSuccess()
    } catch (e: Exception) {
        DomainError.Unknown(e.message).asFailure()
    }

    @Suppress("TooGenericExceptionCaught")
    override suspend fun getProfile(profileId: String): DomainResult<ProviderProfile> = try {
        remoteDataSource.findById(profileId)?.toDomain()?.asSuccess()
            ?: DomainError.NotFound.asFailure()
    } catch (e: Exception) {
        DomainError.Unknown(e.message).asFailure()
    }

    @Suppress("TooGenericExceptionCaught")
    override suspend fun updateRating(
        profileId: String,
        ratingAverage: Double?,
        ratingCount: Int,
    ): DomainResult<ProviderProfile> = try {
        val existing = remoteDataSource.findById(profileId)
            ?: return DomainError.NotFound.asFailure()
        val updated = existing.toDomain().copy(ratingAverage = ratingAverage, ratingCount = ratingCount)
        remoteDataSource.save(updated.toDto()).toDomain().asSuccess()
    } catch (e: Exception) {
        DomainError.Unknown(e.message).asFailure()
    }
}
