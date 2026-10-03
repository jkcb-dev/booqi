package corp.khin.solutions.booqi.domain.model

import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

/**
 * A span of time within one day, `[start, end)`. Value object. Not self-validating on purpose: a
 * Provider's form can momentarily hold an inverted range (e.g. while editing the end time of a
 * day, Figma P6), and a throwing constructor would crash the screen instead of surfacing a form
 * error. "End must be after start" is enforced as [corp.khin.solutions.booqi.core.common.
 * DomainError.InvalidInput] by the Horario use cases, before any repository call.
 */
data class TimeRange(
    val start: LocalTime,
    val end: LocalTime,
) {
    /** True when [end] is strictly after [start] — the only shape a stored range may have. */
    val isValid: Boolean get() = end > start

    /** Half-open overlap: a range ending exactly when [other] starts does not overlap it. */
    fun overlaps(other: TimeRange): Boolean = start < other.end && other.start < end
}

/**
 * The working hours of one day of the week (Figma P6 — one toggle + one range per day). A
 * schedule has at most one entry per [day]. An inactive day keeps its [hours] so toggling it back
 * on restores what the Provider had typed; its range is never validated or used for slots.
 */
data class DayHours(
    val day: DayOfWeek,
    val isActive: Boolean,
    val hours: TimeRange,
)

/**
 * A date (or part of one) the Provider is not available, overriding the weekly schedule (Figma P7
 * — tap a date to block it). [timeRange] `null` blocks the whole [date]; otherwise only that range
 * within it ("Se bloqueó un día/hora específico", docs/domain/provider-flow.md § Grupo 3).
 */
data class BlockedPeriod(
    val date: LocalDate,
    val timeRange: TimeRange? = null,
)

/**
 * A Provider's recurring weekly schedule plus the specific dates/times blocked on top of it
 * (docs/DOMAIN.md — Availability). Value object; `TimeSlot`s are generated from it by
 * [corp.khin.solutions.booqi.domain.usecase.GenerarTimeSlotsUseCase].
 *
 * References its Provider by [providerId] only — it never embeds a [ProviderProfile] (two
 * separate aggregates). Consequently the "paused" vacation range is **not** part of this class:
 * it lives on [ProviderProfile.pausedRange] (Grupo 1, #12) and is passed to slot generation as an
 * explicit input, so there is a single source of truth for it.
 *
 * Ordering contract (guaranteed by the repository): [weeklyHours] Monday to Sunday,
 * [blockedPeriods] chronological (by date, whole-day before time ranges, then by start time).
 *
 * Lifecycle (docs/domain/provider-flow.md § Grupo 3):
 * - weekly hours set by [corp.khin.solutions.booqi.domain.usecase.DefinirHorarioSemanalUseCase]
 *   and changed by [corp.khin.solutions.booqi.domain.usecase.ModificarHorarioSemanalUseCase];
 * - blocked periods added/removed by
 *   [corp.khin.solutions.booqi.domain.usecase.BloquearFechaHoraUseCase] /
 *   [corp.khin.solutions.booqi.domain.usecase.DesbloquearFechaHoraUseCase];
 * - read by [corp.khin.solutions.booqi.domain.usecase.ObtenerHorarioUseCase].
 */
data class Availability(
    val providerId: String,
    val weeklyHours: List<DayHours> = emptyList(),
    val blockedPeriods: List<BlockedPeriod> = emptyList(),
) {
    /** True once a weekly schedule exists; a Provider who never defined one has no hours at all. */
    val isScheduleDefined: Boolean get() = weeklyHours.isNotEmpty()
}
