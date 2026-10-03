package corp.khin.solutions.booqi.feature.provider

import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.Month

private const val MAX_HOUR = 23
private const val MAX_MINUTE = 59
private const val TWO_DIGITS = 10

/** `H:mm`, `HH:mm`, or the same digits without the colon (`930`, `0930`). */
private val TIME_PATTERN = Regex("""^(\d{1,2}):?(\d{2})$""")

/**
 * Parses what a Provider typed into a time-of-day field into a [LocalTime]; `null` when it isn't
 * a valid 24-hour time, so the caller renders a field error instead of crashing. Lenient on
 * purpose — `9:00`, `09:00`, `900` and `0900` are all 09:00 — because the field is a plain text
 * field (no time-picker dialog per row, see [WeeklyScheduleViewModel]). `24:00` is rejected:
 * the domain's [LocalTime] cannot represent the end of the day.
 */
internal fun parseTimeInput(input: String): LocalTime? {
    val match = TIME_PATTERN.matchEntire(input.trim()) ?: return null
    val hour = match.groupValues[1].toInt()
    val minute = match.groupValues[2].toInt()
    return if (hour <= MAX_HOUR && minute <= MAX_MINUTE) LocalTime(hour, minute) else null
}

/** Inverse of [parseTimeInput] in its canonical shape: `09:05`. */
internal fun formatTime(time: LocalTime): String =
    "${time.hour.padTwo()}:${time.minute.padTwo()}"

private fun Int.padTwo(): String = if (this < TWO_DIGITS) "0$this" else toString()

/** Weekday names as shown on P6 (Monday first). */
internal fun DayOfWeek.spanishName(): String = when (this) {
    DayOfWeek.MONDAY -> "Lunes"
    DayOfWeek.TUESDAY -> "Martes"
    DayOfWeek.WEDNESDAY -> "Miércoles"
    DayOfWeek.THURSDAY -> "Jueves"
    DayOfWeek.FRIDAY -> "Viernes"
    DayOfWeek.SATURDAY -> "Sábado"
    DayOfWeek.SUNDAY -> "Domingo"
}

/** One-letter weekday header of the P7 calendar. */
internal fun DayOfWeek.spanishInitial(): String = when (this) {
    DayOfWeek.MONDAY -> "L"
    DayOfWeek.TUESDAY -> "M"
    DayOfWeek.WEDNESDAY -> "X"
    DayOfWeek.THURSDAY -> "J"
    DayOfWeek.FRIDAY -> "V"
    DayOfWeek.SATURDAY -> "S"
    DayOfWeek.SUNDAY -> "D"
}

internal fun Month.spanishName(): String = when (this) {
    Month.JANUARY -> "Enero"
    Month.FEBRUARY -> "Febrero"
    Month.MARCH -> "Marzo"
    Month.APRIL -> "Abril"
    Month.MAY -> "Mayo"
    Month.JUNE -> "Junio"
    Month.JULY -> "Julio"
    Month.AUGUST -> "Agosto"
    Month.SEPTEMBER -> "Septiembre"
    Month.OCTOBER -> "Octubre"
    Month.NOVEMBER -> "Noviembre"
    Month.DECEMBER -> "Diciembre"
}

/** `25 de diciembre de 2026` — a date as the blocked-dates list shows it. */
internal fun LocalDate.spanishText(): String =
    "$dayOfMonth de ${month.spanishName().lowercase()} de $year"
