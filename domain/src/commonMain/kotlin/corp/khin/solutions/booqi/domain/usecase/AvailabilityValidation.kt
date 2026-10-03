package corp.khin.solutions.booqi.domain.usecase

import corp.khin.solutions.booqi.core.common.DomainError
import corp.khin.solutions.booqi.domain.model.DayHours
import corp.khin.solutions.booqi.domain.model.TimeRange

private const val END_BEFORE_START_MESSAGE = "La hora de fin debe ser posterior a la hora de inicio"

/**
 * Input validation shared by the Horario use cases (docs/domain/provider-flow.md § Grupo 3).
 * Returns the [DomainError.InvalidInput] to hand back *before* any repository call, or `null`
 * when [this] is valid. Only the rules the scenarios state: an active day's end is after its
 * start, and a day appears at most once. An inactive day's range is never checked.
 */
internal fun List<DayHours>.weeklyHoursError(): DomainError.InvalidInput? = when {
    map { it.day }.toSet().size != size ->
        DomainError.InvalidInput("Cada día de la semana solo puede aparecer una vez en el horario")
    any { it.isActive && !it.hours.isValid } -> DomainError.InvalidInput(END_BEFORE_START_MESSAGE)
    else -> null
}

/** `null` when [this] is a valid blocked time range; the error otherwise. */
internal fun TimeRange.blockedRangeError(): DomainError.InvalidInput? =
    if (isValid) null else DomainError.InvalidInput(END_BEFORE_START_MESSAGE)
