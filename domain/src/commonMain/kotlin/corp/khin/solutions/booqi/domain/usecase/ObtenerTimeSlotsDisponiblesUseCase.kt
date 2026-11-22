package corp.khin.solutions.booqi.domain.usecase

import corp.khin.solutions.booqi.core.common.DomainResult
import corp.khin.solutions.booqi.core.common.flatMap
import corp.khin.solutions.booqi.core.common.map
import corp.khin.solutions.booqi.domain.model.BookingStatus
import corp.khin.solutions.booqi.domain.model.DateRange
import corp.khin.solutions.booqi.domain.model.TimeSlot
import corp.khin.solutions.booqi.domain.repository.AvailabilityRepository
import corp.khin.solutions.booqi.domain.repository.BookingRepository
import corp.khin.solutions.booqi.domain.repository.ProviderProfileRepository
import kotlin.time.Clock
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant

/**
 * Escenarios (docs/domain/provider-flow.md § Grupo 4): "Un Cliente solicita una reserva" — "el
 * TimeSlot deja de estar disponible para otros Clientes mientras la solicitud está pendiente" — and
 * the "vuelve a estar disponible" clauses of rejecting, expiring and cancelling. This is the
 * follow-up deferred in #16: [GenerarTimeSlotsUseCase]'s output (which stays pure and unchanged)
 * **minus** every slot overlapping a REQUESTED or CONFIRMED Booking of the Provider.
 *
 * Reads the Provider's [Availability][corp.khin.solutions.booqi.domain.model.Availability], their
 * profile's `pausedRange` (read here, not passed in, so the booking flow can't be fed a stale or
 * forged pause) and their active Bookings. Overlap is by time interval, not by equal start: a
 * 30-minute service's slot is unavailable while a 60-minute Booking of another Service covers it
 * (half-open — back-to-back is fine). Rejected, expired and cancelled Bookings (and completed ones,
 * which are in the past) no longer hold a slot, so their slots reappear.
 *
 * An accepted Booking is honored even if its time later falls outside the weekly hours — the
 * subtraction only ever removes slots. Result order is [GenerarTimeSlotsUseCase]'s (date, then
 * start). A missing profile is `NotFound`; `durationMinutes <= 0` is `InvalidInput`.
 *
 * **Past slots (#67):** a slot whose start is not strictly after "now" is dropped. "Now" is the
 * injected [clock] and the wall-clock slot is read in [timeZone] (default: the device's current zone,
 * as the booking screens do) — there is no per-Provider timezone feature yet, so a Provider in
 * another zone is compared in the device's zone.
 */
class ObtenerTimeSlotsDisponiblesUseCase(
    private val availability: AvailabilityRepository,
    private val profiles: ProviderProfileRepository,
    private val bookings: BookingRepository,
    private val generateTimeSlots: GenerarTimeSlotsUseCase,
    private val clock: Clock = Clock.System,
    private val timeZone: TimeZone = TimeZone.currentSystemDefault(),
) {
    suspend operator fun invoke(
        providerId: String,
        durationMinutes: Int,
        range: DateRange,
    ): DomainResult<List<TimeSlot>> =
        profiles.getProfile(providerId).flatMap { profile ->
            availability.getAvailability(providerId).flatMap { schedule ->
                generateTimeSlots(schedule, durationMinutes, range, profile.pausedRange)
            }
        }.flatMap { slots ->
            bookings.getBookingsByProvider(providerId, ACTIVE_STATUSES).map { active ->
                slots.filter { slot ->
                    val start = LocalDateTime(slot.date, slot.start)
                    !hasStarted(slot) && active.none { it.occupies(start, durationMinutes) }
                }
            }
        }

    /** True when [slot] starts at or before "now" (same clock and zone [invoke] filters with). */
    fun hasStarted(slot: TimeSlot): Boolean =
        LocalDateTime(slot.date, slot.start).toInstant(timeZone) <= clock.now()

    private companion object {
        val ACTIVE_STATUSES = BookingStatus.entries.filterTo(mutableSetOf()) { it.occupiesSlot }
    }
}
