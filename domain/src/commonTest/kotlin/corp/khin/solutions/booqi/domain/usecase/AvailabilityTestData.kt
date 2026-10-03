package corp.khin.solutions.booqi.domain.usecase

import corp.khin.solutions.booqi.core.common.DomainResult
import corp.khin.solutions.booqi.domain.model.DayHours
import corp.khin.solutions.booqi.domain.model.TimeRange
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

/** Shared builders/constants for the Grupo 3 tests. 2026-10-05 is a Monday. */
internal val MONDAY_DATE = LocalDate(2026, 10, 5)
internal val FRIDAY_CHRISTMAS = LocalDate(2026, 12, 25)

internal fun time(hour: Int, minute: Int = 0) = LocalTime(hour, minute)

internal fun range(startHour: Int, endHour: Int) = TimeRange(time(startHour), time(endHour))

internal fun workday(day: DayOfWeek, startHour: Int = 9, endHour: Int = 17, active: Boolean = true) =
    DayHours(day, active, range(startHour, endHour))

/** Unwraps a success, failing the test with the error otherwise. */
internal fun <T> DomainResult<T>.value(): T = when (this) {
    is DomainResult.Success -> value
    is DomainResult.Failure -> error("Expected success but was $error")
}
