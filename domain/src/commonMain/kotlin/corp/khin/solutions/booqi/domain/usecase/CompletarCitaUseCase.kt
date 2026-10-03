package corp.khin.solutions.booqi.domain.usecase

import corp.khin.solutions.booqi.core.common.DomainResult
import corp.khin.solutions.booqi.core.common.flatMap
import corp.khin.solutions.booqi.domain.model.Booking
import corp.khin.solutions.booqi.domain.repository.BookingRepository
import kotlinx.datetime.Clock

/**
 * Escenario: "El Proveedor marca una cita confirmada como completada" (docs/domain/provider-flow.md
 * § Grupo 4): CONFIRMED → COMPLETED, always manual — nothing completes a Booking automatically.
 *
 * Only the status is checked (via [Booking.complete]); the scenario states no rule about *when*.
 * Figma P10 disables the button until the appointment's hour has passed — that is a UI
 * affordance (it can compute the end from `scheduledAt` + `durationMinutesSnapshot`), not a domain
 * invariant here, since there is no timezone to compare the wall-clock `scheduledAt` against.
 */
class CompletarCitaUseCase(
    private val repository: BookingRepository,
    private val clock: Clock = Clock.System,
) {
    suspend operator fun invoke(bookingId: String): DomainResult<Booking> =
        repository.getBooking(bookingId)
            .flatMap { it.complete(clock.now()) }
            .flatMap { repository.updateBooking(it) }
}
