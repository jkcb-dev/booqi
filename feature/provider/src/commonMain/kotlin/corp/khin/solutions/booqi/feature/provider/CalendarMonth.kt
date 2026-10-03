package corp.khin.solutions.booqi.feature.provider

import corp.khin.solutions.booqi.domain.model.BlockedPeriod
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.plus

private const val DAYS_IN_WEEK = 7

/** The first day of [date]'s month — how the calendar identifies the month it shows. */
internal fun firstOfMonth(date: LocalDate): LocalDate = LocalDate(date.year, date.month, 1)

/** [firstOfMonth] moved by [months] (negative = back). */
internal fun LocalDate.shiftedByMonths(months: Int): LocalDate =
    firstOfMonth(this).plus(months, DateTimeUnit.MONTH)

/**
 * The month starting at [firstOfMonth] as calendar weeks of seven cells, Monday first (the app's
 * week start, docs/design/SCREENS.md P6): `null` pads the days of the neighbouring months.
 */
internal fun monthWeeks(firstOfMonth: LocalDate): List<List<LocalDate?>> {
    val leadingBlanks = firstOfMonth.dayOfWeek.isoDayNumber - DayOfWeek.MONDAY.isoDayNumber
    val daysInMonth = firstOfMonth.daysUntil(firstOfMonth.plus(1, DateTimeUnit.MONTH))
    val cells: List<LocalDate?> = List(leadingBlanks) { null } +
        List(daysInMonth) { firstOfMonth.plus(it, DateTimeUnit.DAY) }
    return cells.chunked(DAYS_IN_WEEK).map { week -> week + List(DAYS_IN_WEEK - week.size) { null } }
}

/** How the calendar paints a date, from the Provider's blocked periods. */
internal enum class DateBlockState { Free, PartiallyBlocked, FullyBlocked }

internal fun List<BlockedPeriod>.blockStateOf(date: LocalDate): DateBlockState {
    val onDate = filter { it.date == date }
    return when {
        onDate.any { it.timeRange == null } -> DateBlockState.FullyBlocked
        onDate.isNotEmpty() -> DateBlockState.PartiallyBlocked
        else -> DateBlockState.Free
    }
}
