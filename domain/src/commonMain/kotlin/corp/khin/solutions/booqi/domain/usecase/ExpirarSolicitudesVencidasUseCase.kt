package corp.khin.solutions.booqi.domain.usecase

import corp.khin.solutions.booqi.core.common.DomainResult
import corp.khin.solutions.booqi.core.common.asSuccess
import corp.khin.solutions.booqi.core.common.flatMap
import corp.khin.solutions.booqi.domain.model.Booking
import corp.khin.solutions.booqi.domain.repository.BookingRepository
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * Escenario: "Una solicitud expira sin respuesta" (docs/domain/provider-flow.md § Grupo 4):
 * every REQUESTED Booking whose `requestedAt + 24h <= now` (the boundary instant counts as
 * expired) moves to EXPIRED. Returns how many were expired. The TimeSlot is freed implicitly.
 *
 * This is the whole domain side of `ExpirarSolicitud`: it is a plain, idempotent use case so *any*
 * trigger can call it (a server-side schedule or a client-side periodic call — see
 * docs/ARCHITECTURE.md § Booking expiry trigger); no scheduler is built here. Running it again
 * finds nothing left to expire. Each Booking goes through [Booking.expire], which re-checks the
 * deadline. Stops at the first persistence failure and returns it — the untouched ones stay
 * REQUESTED and are picked up by the next run. The Customer's fixed system message is deferred
 * with the rest of notifications.
 */
class ExpirarSolicitudesVencidasUseCase(
    private val repository: BookingRepository,
    private val clock: Clock = Clock.System,
) {
    suspend operator fun invoke(): DomainResult<Int> {
        val now = clock.now()
        return repository.getPendingRequestedAtOrBefore(now - Booking.RESPONSE_WINDOW)
            .flatMap { overdue -> expireAll(overdue, now) }
    }

    private suspend fun expireAll(overdue: List<Booking>, now: Instant): DomainResult<Int> {
        var expired = 0
        for (booking in overdue) {
            val result = booking.expire(now).flatMap { repository.updateBooking(it) }
            if (result is DomainResult.Failure) return result
            expired++
        }
        return expired.asSuccess()
    }
}
