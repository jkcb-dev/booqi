package corp.khin.solutions.booqi.data.repository

import corp.khin.solutions.booqi.core.common.DomainResult
import corp.khin.solutions.booqi.data.datasource.FakeAvailabilityRemoteDataSource
import corp.khin.solutions.booqi.domain.model.Availability
import corp.khin.solutions.booqi.domain.model.BlockedPeriod
import corp.khin.solutions.booqi.domain.model.DayHours
import corp.khin.solutions.booqi.domain.model.TimeRange
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Covers what [AvailabilityRepositoryImpl] adds over the datasource: DTO round-trip through the
 * mappers, the documented ordering contract, replace/idempotency semantics and per-provider
 * isolation. Domain-level BDD scenarios live in `domain`'s use case tests.
 */
class AvailabilityRepositoryImplTest {

    private val repository = AvailabilityRepositoryImpl(FakeAvailabilityRemoteDataSource())

    private val christmas = LocalDate(2026, 12, 25)
    private val nineToFive = TimeRange(LocalTime(9, 0), LocalTime(17, 30))

    private fun Any.ok(): Availability = (this as DomainResult.Success<*>).value as Availability

    @Test
    fun `a provider with nothing stored gets an empty availability`() = runTest {
        val availability = repository.getAvailability("p1").ok()

        assertEquals(Availability("p1"), availability)
    }

    @Test
    fun `weekly hours round trip and come back Monday to Sunday`() = runTest {
        val hours = listOf(
            DayHours(DayOfWeek.SUNDAY, false, nineToFive),
            DayHours(DayOfWeek.MONDAY, true, nineToFive),
            DayHours(DayOfWeek.THURSDAY, true, TimeRange(LocalTime(8, 15), LocalTime(12, 0))),
        )

        val saved = repository.saveWeeklyHours("p1", hours).ok()

        assertEquals(hours.sortedBy { it.day.ordinal }, saved.weeklyHours)
    }

    @Test
    fun `saving weekly hours replaces the previous schedule and keeps blocked periods`() = runTest {
        repository.saveWeeklyHours("p1", listOf(DayHours(DayOfWeek.MONDAY, true, nineToFive)))
        repository.addBlockedPeriod("p1", BlockedPeriod(christmas))

        val saved = repository.saveWeeklyHours("p1", listOf(DayHours(DayOfWeek.FRIDAY, true, nineToFive))).ok()

        assertEquals(listOf(DayOfWeek.FRIDAY), saved.weeklyHours.map { it.day })
        assertEquals(listOf(BlockedPeriod(christmas)), saved.blockedPeriods)
    }

    @Test
    fun `blocked periods round trip and come back chronologically with whole days first`() = runTest {
        val range = TimeRange(LocalTime(12, 0), LocalTime(14, 0))
        val earlier = TimeRange(LocalTime(9, 0), LocalTime(10, 0))
        repository.addBlockedPeriod("p1", BlockedPeriod(christmas))
        repository.addBlockedPeriod("p1", BlockedPeriod(LocalDate(2026, 10, 5), range))
        repository.addBlockedPeriod("p1", BlockedPeriod(LocalDate(2026, 10, 5), earlier))
        val saved = repository.addBlockedPeriod("p1", BlockedPeriod(LocalDate(2026, 10, 5))).ok()

        assertEquals(
            listOf(
                BlockedPeriod(LocalDate(2026, 10, 5)),
                BlockedPeriod(LocalDate(2026, 10, 5), earlier),
                BlockedPeriod(LocalDate(2026, 10, 5), range),
                BlockedPeriod(christmas),
            ),
            saved.blockedPeriods,
        )
    }

    @Test
    fun `adding the same blocked period twice stores it once and removing is idempotent`() = runTest {
        repository.addBlockedPeriod("p1", BlockedPeriod(christmas))
        val twice = repository.addBlockedPeriod("p1", BlockedPeriod(christmas)).ok()
        assertEquals(1, twice.blockedPeriods.size)

        repository.removeBlockedPeriod("p1", BlockedPeriod(christmas))
        val again = repository.removeBlockedPeriod("p1", BlockedPeriod(christmas)).ok()

        assertTrue(again.blockedPeriods.isEmpty())
    }

    @Test
    fun `providers do not see each other's schedule`() = runTest {
        repository.saveWeeklyHours("p1", listOf(DayHours(DayOfWeek.MONDAY, true, nineToFive)))
        repository.addBlockedPeriod("p1", BlockedPeriod(christmas))

        assertEquals(Availability("p2"), repository.getAvailability("p2").ok())
    }
}
