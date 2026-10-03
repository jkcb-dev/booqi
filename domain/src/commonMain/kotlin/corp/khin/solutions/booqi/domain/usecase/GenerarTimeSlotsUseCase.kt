package corp.khin.solutions.booqi.domain.usecase

import corp.khin.solutions.booqi.core.common.DomainError
import corp.khin.solutions.booqi.core.common.DomainResult
import corp.khin.solutions.booqi.core.common.asFailure
import corp.khin.solutions.booqi.core.common.asSuccess
import corp.khin.solutions.booqi.domain.model.Availability
import corp.khin.solutions.booqi.domain.model.DateRange
import corp.khin.solutions.booqi.domain.model.TimeRange
import corp.khin.solutions.booqi.domain.model.TimeSlot
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.plus

/**
 * Escenarios (docs/domain/provider-flow.md § Grupo 3): "Se generan los TimeSlots según el horario y
 * la duración del Servicio", "Los días inactivos y los días sin horario no generan TimeSlots" and
 * "Un perfil pausado no genera TimeSlots durante la pausa", plus the two block scenarios.
 *
 * Pure and synchronous — no repository, no I/O: the caller loads the [Availability] (see
 * [ObtenerHorarioUseCase]) and the Service, and passes what it needs. This keeps it trivially
 * testable and free of any dependency on `ProviderProfileRepository` or `Booking`.
 *
 * For each date in [range], in order:
 * - nothing if the date is inside [pausedRange] (vacation mode, `ProviderProfile.pausedRange` —
 *   passed in explicitly so the pause has one source of truth);
 * - nothing if the weekday has no *active* entry in the weekly hours;
 * - nothing if a whole-day [corp.khin.solutions.booqi.domain.model.BlockedPeriod] covers the date;
 * - otherwise slots of [durationMinutes], back to back from the day's start, as long as the
 *   *whole* slot fits before the day's end (a leftover shorter than the duration yields nothing),
 *   skipping any slot that overlaps a blocked time range. Overlap is half-open: a slot ending
 *   exactly when a block starts (or starting when it ends) stays available.
 *
 * Slots are not re-aligned around blocks: the grid is anchored at the day's start, so a block
 * only removes the slots it touches.
 *
 * `durationMinutes <= 0` is [DomainError.InvalidInput] (it would never advance). The result is
 * ordered by date, then start time; an empty list is a valid outcome.
 *
 * **Out of scope — follow-up for #18/#25 (Booking):** slots already taken by a pending or
 * confirmed `Booking` are *not* excluded here, because `Booking` doesn't exist yet. When it does,
 * the booking flow must subtract them from this result (and keep honoring an accepted Booking
 * even if its time later falls outside the weekly hours — "cambiar el horario no cancela citas
 * aceptadas"). Nothing is stubbed for it.
 */
class GenerarTimeSlotsUseCase {

    operator fun invoke(
        availability: Availability,
        durationMinutes: Int,
        range: DateRange,
        pausedRange: DateRange? = null,
    ): DomainResult<List<TimeSlot>> {
        if (durationMinutes <= 0) {
            return DomainError.InvalidInput("La duración debe ser mayor a 0 minutos").asFailure()
        }
        val slots = mutableListOf<TimeSlot>()
        var date = range.start
        while (date <= range.end) {
            if (pausedRange == null || date !in pausedRange) {
                slots += slotsFor(availability, date, durationMinutes)
            }
            date = date.plus(1, DateTimeUnit.DAY)
        }
        return slots.asSuccess()
    }

    private fun slotsFor(availability: Availability, date: LocalDate, durationMinutes: Int): List<TimeSlot> {
        val day = availability.weeklyHours.firstOrNull { it.day == date.dayOfWeek && it.isActive }
        val blocks = availability.blockedPeriods.filter { it.date == date }
        if (day == null || blocks.any { it.timeRange == null }) return emptyList()
        val blockedRanges = blocks.mapNotNull { it.timeRange }

        val step = durationMinutes * SECONDS_PER_MINUTE
        val dayEnd = day.hours.end.toSecondOfDay()
        val slots = mutableListOf<TimeSlot>()
        var start = day.hours.start.toSecondOfDay()
        while (start + step <= dayEnd) {
            val candidate = TimeRange(LocalTime.fromSecondOfDay(start), LocalTime.fromSecondOfDay(start + step))
            if (blockedRanges.none { it.overlaps(candidate) }) {
                slots += TimeSlot(availability.providerId, date, candidate.start)
            }
            start += step
        }
        return slots
    }

    private companion object {
        const val SECONDS_PER_MINUTE = 60
    }
}
