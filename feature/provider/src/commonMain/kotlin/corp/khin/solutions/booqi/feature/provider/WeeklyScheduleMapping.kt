package corp.khin.solutions.booqi.feature.provider

import corp.khin.solutions.booqi.domain.model.Availability
import corp.khin.solutions.booqi.domain.model.DayHours
import corp.khin.solutions.booqi.domain.model.TimeRange
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalTime

private const val DEFAULT_START_HOUR = 9
private const val DEFAULT_END_HOUR = 18

/** Hours a day shows when the Provider never defined it (and the fallback for an inactive row
 * whose text is not a time — an inactive day's range is never validated). */
internal val DEFAULT_HOURS = TimeRange(LocalTime(DEFAULT_START_HOUR, 0), LocalTime(DEFAULT_END_HOUR, 0))

internal const val INVALID_TIME_MESSAGE = "Usá el formato HH:mm, por ejemplo 09:30"

/** Monday to Sunday, the order of P6 and of the domain's ordering contract. */
private val WEEK: List<DayOfWeek> = DayOfWeek.entries

/** The seven rows for [Availability.weeklyHours]: stored days as stored, the rest inactive. */
internal fun List<DayHours>.toRows(): List<ScheduleDayRowState> = WEEK.map { day ->
    val stored = firstOrNull { it.day == day } ?: DayHours(day, isActive = false, hours = DEFAULT_HOURS)
    ScheduleDayRowState(
        day = day,
        isActive = stored.isActive,
        startInput = formatTime(stored.hours.start),
        endInput = formatTime(stored.hours.end),
    )
}

internal fun Availability.toRows(): List<ScheduleDayRowState> = weeklyHours.toRows()

/** What a day looks like in the baseline: stored, or "not worked" with the default hours. */
internal fun List<DayHours>.baselineFor(day: DayOfWeek): DayHours =
    firstOrNull { it.day == day } ?: DayHours(day, isActive = false, hours = DEFAULT_HOURS)

/**
 * The domain value of a row, or `null` when an **active** row has text that is not a time. An
 * inactive row never fails: if its text is unusable it falls back to [fallback] (the saved
 * hours), as the domain does not validate inactive days either.
 */
internal fun ScheduleDayRowState.toDayHours(fallback: TimeRange): DayHours? {
    val start = parseTimeInput(startInput)
    val end = parseTimeInput(endInput)
    return when {
        start != null && end != null -> DayHours(day, isActive, TimeRange(start, end))
        isActive -> null
        else -> DayHours(day, isActive = false, hours = fallback)
    }
}

/** True when any row differs from [saved]. A row that does not parse counts as a change (it is
 * about to be reported as an error), so Save stays available to show that error. */
internal fun rowsDiffer(rows: List<ScheduleDayRowState>, saved: List<DayHours>): Boolean =
    rows.any { row ->
        val baseline = saved.baselineFor(row.day)
        row.toDayHours(fallback = baseline.hours)?.let { it != baseline } ?: true
    }

/** Rows that fail to parse, mapped to the inline message. */
internal fun List<ScheduleDayRowState>.withParseErrors(saved: List<DayHours>): List<ScheduleDayRowState> =
    map { row ->
        val parsed = row.toDayHours(fallback = saved.baselineFor(row.day).hours)
        row.copy(error = if (parsed == null) INVALID_TIME_MESSAGE else null)
    }

/** Marks the active rows whose parsed range ends at or before its start — the rows a domain
 * `InvalidInput` is about. Returns the rows unchanged (all errors cleared) when none match. */
internal fun List<ScheduleDayRowState>.withRangeErrors(message: String): List<ScheduleDayRowState> =
    map { row ->
        val start = parseTimeInput(row.startInput)
        val end = parseTimeInput(row.endInput)
        val inverted = row.isActive && start != null && end != null && !TimeRange(start, end).isValid
        row.copy(error = if (inverted) message else null)
    }

/** What pressing "Guardar horario" amounts to, decided from the state alone. */
internal sealed interface ScheduleDraft {
    /** Some active row's text is not a time; nothing is sent, these rows carry the errors. */
    data class Invalid(val rows: List<ScheduleDayRowState>) : ScheduleDraft

    /** An existing schedule with no row changed. */
    data object NothingToSave : ScheduleDraft

    /** No schedule yet: the whole week, as `DefinirHorarioSemanal`. */
    data class Define(val week: List<DayHours>) : ScheduleDraft

    /** A schedule exists: only the changed days, as `ModificarHorarioSemanal`. */
    data class Modify(val changes: List<DayHours>) : ScheduleDraft
}

internal fun WeeklyScheduleUiState.toDraft(): ScheduleDraft {
    val parsed = rows.map { it.toDayHours(fallback = savedHours.baselineFor(it.day).hours) }
    val week = parsed.filterNotNull()
    return when {
        week.size != parsed.size -> ScheduleDraft.Invalid(rows.withParseErrors(savedHours))
        !isScheduleDefined -> ScheduleDraft.Define(week)
        else -> {
            val changes = week.filter { it != savedHours.baselineFor(it.day) }
            if (changes.isEmpty()) ScheduleDraft.NothingToSave else ScheduleDraft.Modify(changes)
        }
    }
}
