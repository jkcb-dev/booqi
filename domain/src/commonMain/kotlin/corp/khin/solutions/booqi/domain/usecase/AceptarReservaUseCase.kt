package corp.khin.solutions.booqi.domain.usecase

import corp.khin.solutions.booqi.core.common.DomainResult
import corp.khin.solutions.booqi.core.common.flatMap
import corp.khin.solutions.booqi.domain.model.Booking
import corp.khin.solutions.booqi.domain.repository.BookingRepository
import kotlin.time.Clock

/**
 * Escenario: "El Proveedor acepta una solicitud" (docs/domain/provider-flow.md § Grupo 4):
 * REQUESTED → CONFIRMED.
 *
 * The transition rule lives in [Booking.confirm] (single state machine): accepting a Booking that
 * is not pending, or whose 24h window already lapsed, is a
 * [corp.khin.solutions.booqi.core.common.DomainError.InvalidInput] and nothing is persisted.
 * "El Cliente es notificado" is deferred — no notification system exists yet. [clock] exists so
 * tests can fake time; production uses [Clock.System].
 */
class AceptarReservaUseCase(
    private val repository: BookingRepository,
    private val clock: Clock = Clock.System,
) {
    suspend operator fun invoke(bookingId: String): DomainResult<Booking> =
        repository.getBooking(bookingId)
            .flatMap { it.confirm(clock.now()) }
            .flatMap { repository.updateBooking(it) }
}
