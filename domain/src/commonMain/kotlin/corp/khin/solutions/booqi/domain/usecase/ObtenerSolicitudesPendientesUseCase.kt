package corp.khin.solutions.booqi.domain.usecase

import corp.khin.solutions.booqi.core.common.DomainResult
import corp.khin.solutions.booqi.core.common.map
import corp.khin.solutions.booqi.domain.model.Booking
import corp.khin.solutions.booqi.domain.model.BookingStatus
import corp.khin.solutions.booqi.domain.repository.BookingRepository
import kotlin.time.Clock

/**
 * Query behind the Provider's request inbox (Figma P8, and P9's detail source list): the
 * REQUESTED Bookings of [providerId] that the Provider can still act on. Escenario: "Un Cliente
 * solicita una reserva" (the Provider "tiene 24 horas para responder").
 *
 * Order: oldest request first (`requestedAt`, then id) — the one closest to expiring is on top.
 * A request already past its 24h window but not yet swept to EXPIRED is left out, since accepting
 * or rejecting it would fail ([Booking.isResponseOverdue]). No pending requests yields an empty
 * list, not an error.
 */
class ObtenerSolicitudesPendientesUseCase(
    private val repository: BookingRepository,
    private val clock: Clock = Clock.System,
) {
    suspend operator fun invoke(providerId: String): DomainResult<List<Booking>> {
        val now = clock.now()
        return repository.getBookingsByProvider(providerId, setOf(BookingStatus.REQUESTED)).map { pending ->
            pending
                .filterNot { it.isResponseOverdue(now) }
                .sortedWith(compareBy({ it.requestedAt }, { it.id }))
        }
    }
}
