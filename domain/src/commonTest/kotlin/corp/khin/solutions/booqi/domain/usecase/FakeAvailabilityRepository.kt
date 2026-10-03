package corp.khin.solutions.booqi.domain.usecase

import corp.khin.solutions.booqi.core.common.DomainResult
import corp.khin.solutions.booqi.core.common.asSuccess
import corp.khin.solutions.booqi.domain.model.Availability
import corp.khin.solutions.booqi.domain.model.BlockedPeriod
import corp.khin.solutions.booqi.domain.model.DayHours
import corp.khin.solutions.booqi.domain.repository.AvailabilityRepository
import kotlinx.datetime.isoDayNumber

/**
 * Hand-written fake shared by the Grupo 3 use case tests — a minimal in-memory
 * [AvailabilityRepository] good enough to exercise the BDD scenarios in
 * docs/domain/provider-flow.md § Grupo 3 without any real I/O. Honors the documented ordering
 * contract. [writeCount] lets tests assert that an invalid submission never reached persistence.
 */
class FakeAvailabilityRepository : AvailabilityRepository {

    private val weeklyByProvider = mutableMapOf<String, List<DayHours>>()
    private val blockedByProvider = mutableMapOf<String, List<BlockedPeriod>>()

    /** Number of mutating calls received (save/add/remove). */
    var writeCount = 0
        private set

    override suspend fun getAvailability(providerId: String): DomainResult<Availability> = snapshot(providerId).asSuccess()

    override suspend fun saveWeeklyHours(providerId: String, weeklyHours: List<DayHours>): DomainResult<Availability> {
        writeCount++
        weeklyByProvider[providerId] = weeklyHours
        return snapshot(providerId).asSuccess()
    }

    override suspend fun addBlockedPeriod(providerId: String, period: BlockedPeriod): DomainResult<Availability> {
        writeCount++
        val current = blockedByProvider[providerId].orEmpty()
        if (period !in current) blockedByProvider[providerId] = current + period
        return snapshot(providerId).asSuccess()
    }

    override suspend fun removeBlockedPeriod(providerId: String, period: BlockedPeriod): DomainResult<Availability> {
        writeCount++
        blockedByProvider[providerId] = blockedByProvider[providerId].orEmpty() - period
        return snapshot(providerId).asSuccess()
    }

    private fun snapshot(providerId: String) = Availability(
        providerId = providerId,
        weeklyHours = weeklyByProvider[providerId].orEmpty().sortedBy { it.day.isoDayNumber },
        blockedPeriods = blockedByProvider[providerId].orEmpty()
            .sortedWith(compareBy({ it.date }, { it.timeRange?.start }, { it.timeRange?.end })),
    )
}
