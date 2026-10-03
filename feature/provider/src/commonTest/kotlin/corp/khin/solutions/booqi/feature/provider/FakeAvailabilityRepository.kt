package corp.khin.solutions.booqi.feature.provider

import corp.khin.solutions.booqi.core.common.DomainError
import corp.khin.solutions.booqi.core.common.DomainResult
import corp.khin.solutions.booqi.core.common.asFailure
import corp.khin.solutions.booqi.core.common.asSuccess
import corp.khin.solutions.booqi.domain.model.Availability
import corp.khin.solutions.booqi.domain.model.BlockedPeriod
import corp.khin.solutions.booqi.domain.model.DayHours
import corp.khin.solutions.booqi.domain.repository.AvailabilityRepository
import kotlinx.datetime.isoDayNumber

/**
 * Minimal in-memory fake for [AvailabilityRepository], scoped to the schedule ViewModels' reducer
 * tests. Same shape as `domain`'s own `FakeAvailabilityRepository` (commonTest sourceSets aren't
 * shared across Gradle modules), kept in sync by hand, plus [readFailure]/[writeFailure] to
 * simulate failing calls. [writeCount] lets tests assert that an invalid submission never reached
 * persistence.
 */
class FakeAvailabilityRepository : AvailabilityRepository {

    private val weeklyByProvider = mutableMapOf<String, List<DayHours>>()
    private val blockedByProvider = mutableMapOf<String, List<BlockedPeriod>>()

    /** When set, [getAvailability] fails with it. */
    var readFailure: DomainError? = null

    /** When set, every mutating call fails with it (and is not counted as a write). */
    var writeFailure: DomainError? = null

    /** Number of mutating calls that reached persistence (save/add/remove). */
    var writeCount = 0
        private set

    /** What is stored for [providerId], bypassing failures — for assertions and seeding. */
    fun stored(providerId: String = TEMPORARY_PROVIDER_ID): Availability = snapshot(providerId)

    fun seedWeekly(hours: List<DayHours>, providerId: String = TEMPORARY_PROVIDER_ID) {
        weeklyByProvider[providerId] = hours
    }

    fun seedBlocked(periods: List<BlockedPeriod>, providerId: String = TEMPORARY_PROVIDER_ID) {
        blockedByProvider[providerId] = periods
    }

    override suspend fun getAvailability(providerId: String): DomainResult<Availability> =
        readFailure?.asFailure() ?: snapshot(providerId).asSuccess()

    override suspend fun saveWeeklyHours(providerId: String, weeklyHours: List<DayHours>): DomainResult<Availability> =
        write(providerId) { weeklyByProvider[providerId] = weeklyHours }

    override suspend fun addBlockedPeriod(providerId: String, period: BlockedPeriod): DomainResult<Availability> =
        write(providerId) {
            val current = blockedByProvider[providerId].orEmpty()
            if (period !in current) blockedByProvider[providerId] = current + period
        }

    override suspend fun removeBlockedPeriod(providerId: String, period: BlockedPeriod): DomainResult<Availability> =
        write(providerId) { blockedByProvider[providerId] = blockedByProvider[providerId].orEmpty() - period }

    private fun write(providerId: String, change: () -> Unit): DomainResult<Availability> {
        writeFailure?.let { return it.asFailure() }
        writeCount++
        change()
        return snapshot(providerId).asSuccess()
    }

    private fun snapshot(providerId: String) = Availability(
        providerId = providerId,
        weeklyHours = weeklyByProvider[providerId].orEmpty().sortedBy { it.day.isoDayNumber },
        blockedPeriods = blockedByProvider[providerId].orEmpty()
            .sortedWith(compareBy({ it.date }, { it.timeRange?.start }, { it.timeRange?.end })),
    )
}
