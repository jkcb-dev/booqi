package corp.khin.solutions.booqi.data.mapper

import corp.khin.solutions.booqi.data.dto.BlockedDateDto
import corp.khin.solutions.booqi.data.dto.WeeklyHoursDto
import corp.khin.solutions.booqi.domain.model.BlockedPeriod
import corp.khin.solutions.booqi.domain.model.DayHours
import corp.khin.solutions.booqi.domain.model.TimeRange
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.isoDayNumber

fun WeeklyHoursDto.toDomain(): DayHours = DayHours(
    day = DayOfWeek(dayOfWeek),
    isActive = isActive,
    hours = TimeRange(LocalTime.parse(startTime), LocalTime.parse(endTime)),
)

fun DayHours.toDto(): WeeklyHoursDto = WeeklyHoursDto(
    dayOfWeek = day.isoDayNumber,
    isActive = isActive,
    startTime = hours.start.toString(),
    endTime = hours.end.toString(),
)

fun BlockedDateDto.toDomain(): BlockedPeriod {
    val start = startTime
    val end = endTime
    return BlockedPeriod(
        date = LocalDate.parse(blockedDate),
        timeRange = if (start != null && end != null) {
            TimeRange(LocalTime.parse(start), LocalTime.parse(end))
        } else {
            null
        },
    )
}

fun BlockedPeriod.toDto(): BlockedDateDto = BlockedDateDto(
    blockedDate = date.toString(),
    startTime = timeRange?.start?.toString(),
    endTime = timeRange?.end?.toString(),
)
