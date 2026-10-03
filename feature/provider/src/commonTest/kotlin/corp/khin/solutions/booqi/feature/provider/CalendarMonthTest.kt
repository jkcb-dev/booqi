package corp.khin.solutions.booqi.feature.provider

import corp.khin.solutions.booqi.domain.model.BlockedPeriod
import corp.khin.solutions.booqi.domain.model.TimeRange
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** The pure month-grid and blocked-state helpers behind the P7 calendar. */
class CalendarMonthTest {

    @Test
    fun `a month grid starts on Monday and pads to whole weeks`() {
        // 1 October 2026 is a Thursday: three blanks, 31 days, padded to 35 cells.
        val weeks = monthWeeks(LocalDate(2026, 10, 1))

        assertEquals(5, weeks.size)
        assertTrue(weeks.all { it.size == 7 })
        assertEquals(listOf(null, null, null, LocalDate(2026, 10, 1)), weeks[0].take(4))
        assertEquals(LocalDate(2026, 10, 31), weeks.last().filterNotNull().last())
        assertEquals(31, weeks.flatten().count { it != null })
        assertNull(weeks.last().last())
    }

    @Test
    fun `a month starting on Monday with 28 days is exactly four full weeks`() {
        val weeks = monthWeeks(LocalDate(2027, 2, 1))

        assertEquals(4, weeks.size)
        assertEquals(LocalDate(2027, 2, 1), weeks[0][0])
        assertEquals(LocalDate(2027, 2, 28), weeks[3][6])
    }

    @Test
    fun `leap february has 29 days`() {
        assertEquals(29, monthWeeks(LocalDate(2024, 2, 1)).flatten().count { it != null })
    }

    @Test
    fun `a month ending on Sunday can need six rows when it starts late in the week`() {
        // 1 August 2026 is a Saturday: five blanks + 31 days = 36 cells -> six weeks.
        assertEquals(6, monthWeeks(LocalDate(2026, 8, 1)).size)
    }

    @Test
    fun `shifting months rolls the year and always lands on the first`() {
        assertEquals(LocalDate(2027, 1, 1), LocalDate(2026, 12, 17).shiftedByMonths(1))
        assertEquals(LocalDate(2025, 12, 1), LocalDate(2026, 1, 31).shiftedByMonths(-1))
        assertEquals(LocalDate(2026, 10, 1), LocalDate(2026, 10, 17).shiftedByMonths(0))
        assertEquals(LocalDate(2026, 10, 1), firstOfMonth(LocalDate(2026, 10, 17)))
    }

    @Test
    fun `a date is free partially blocked or fully blocked from the periods on it`() {
        val day = LocalDate(2026, 12, 25)
        val range = BlockedPeriod(day, TimeRange(LocalTime(12, 0), LocalTime(14, 0)))
        val whole = BlockedPeriod(day)

        assertEquals(DateBlockState.Free, emptyList<BlockedPeriod>().blockStateOf(day))
        assertEquals(DateBlockState.Free, listOf(BlockedPeriod(LocalDate(2026, 12, 26))).blockStateOf(day))
        assertEquals(DateBlockState.PartiallyBlocked, listOf(range).blockStateOf(day))
        assertEquals(DateBlockState.FullyBlocked, listOf(whole).blockStateOf(day))
        assertEquals(DateBlockState.FullyBlocked, listOf(range, whole).blockStateOf(day))
    }
}
