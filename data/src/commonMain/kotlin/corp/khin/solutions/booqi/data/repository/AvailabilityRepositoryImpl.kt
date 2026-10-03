package corp.khin.solutions.booqi.data.repository

import corp.khin.solutions.booqi.core.common.DomainError
import corp.khin.solutions.booqi.core.common.DomainResult
import corp.khin.solutions.booqi.core.common.asFailure
import corp.khin.solutions.booqi.core.common.asSuccess
import corp.khin.solutions.booqi.data.datasource.AvailabilityRemoteDataSource
import corp.khin.solutions.booqi.data.mapper.toDomain
import corp.khin.solutions.booqi.data.mapper.toDto
import corp.khin.solutions.booqi.domain.model.Availability
import corp.khin.solutions.booqi.domain.model.BlockedPeriod
import corp.khin.solutions.booqi.domain.model.DayHours
import corp.khin.solutions.booqi.domain.repository.AvailabilityRepository
import kotlinx.datetime.isoDayNumber

/**
 * Assembles an [Availability] from the two datasource tables and enforces the ordering contract
 * documented on it (weekly hours Monday→Sunday, blocked periods chronological, whole-day first),
 * so a real datasource can return rows in any order.
 */
class AvailabilityRepositoryImpl(
    private val remoteDataSource: AvailabilityRemoteDataSource,
) : AvailabilityRepository {

    // Deliberate: this boundary is where every real exception (network, serialization, a
    // malformed stored time, ...) gets translated into a DomainError — see core:common's
    // DomainResult docs. Same pattern as ServiceRepositoryImpl.
    @Suppress("TooGenericExceptionCaught")
    override suspend fun getAvailability(providerId: String): DomainResult<Availability> = try {
        load(providerId).asSuccess()
    } catch (e: Exception) {
        DomainError.Unknown(e.message).asFailure()
    }

    @Suppress("TooGenericExceptionCaught")
    override suspend fun saveWeeklyHours(
        providerId: String,
        weeklyHours: List<DayHours>,
    ): DomainResult<Availability> = try {
        remoteDataSource.replaceWeeklyHours(providerId, weeklyHours.map { it.toDto() })
        load(providerId).asSuccess()
    } catch (e: Exception) {
        DomainError.Unknown(e.message).asFailure()
    }

    @Suppress("TooGenericExceptionCaught")
    override suspend fun addBlockedPeriod(
        providerId: String,
        period: BlockedPeriod,
    ): DomainResult<Availability> = try {
        remoteDataSource.addBlockedDate(providerId, period.toDto())
        load(providerId).asSuccess()
    } catch (e: Exception) {
        DomainError.Unknown(e.message).asFailure()
    }

    @Suppress("TooGenericExceptionCaught")
    override suspend fun removeBlockedPeriod(
        providerId: String,
        period: BlockedPeriod,
    ): DomainResult<Availability> = try {
        remoteDataSource.removeBlockedDate(providerId, period.toDto())
        load(providerId).asSuccess()
    } catch (e: Exception) {
        DomainError.Unknown(e.message).asFailure()
    }

    private suspend fun load(providerId: String): Availability = Availability(
        providerId = providerId,
        weeklyHours = remoteDataSource.findWeeklyHours(providerId)
            .map { it.toDomain() }
            .sortedBy { it.day.isoDayNumber },
        // compareBy treats a null start (whole-day block) as smallest, so it sorts first.
        blockedPeriods = remoteDataSource.findBlockedDates(providerId)
            .map { it.toDomain() }
            .sortedWith(compareBy({ it.date }, { it.timeRange?.start }, { it.timeRange?.end })),
    )
}
