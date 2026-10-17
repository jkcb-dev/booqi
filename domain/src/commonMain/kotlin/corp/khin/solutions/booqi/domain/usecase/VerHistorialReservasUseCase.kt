package corp.khin.solutions.booqi.domain.usecase

import corp.khin.solutions.booqi.core.common.DomainError
import corp.khin.solutions.booqi.core.common.DomainResult
import corp.khin.solutions.booqi.core.common.asFailure
import corp.khin.solutions.booqi.core.common.flatMap
import corp.khin.solutions.booqi.core.common.map
import corp.khin.solutions.booqi.domain.model.Booking
import corp.khin.solutions.booqi.domain.model.BookingHistory
import corp.khin.solutions.booqi.domain.repository.BookingRepository

/**
 * Escenario: "El Cliente consulta su historial de reservas" (docs/domain/customer-flow.md § Grupo
 * 3, "Mis reservas"): every Booking of the signed-in Customer (`customerId == user.id`), **separated
 * by status** into a [BookingHistory]. Needs a session — `Unauthorized` without one. No Bookings is
 * an empty history, not an error. Read-only.
 *
 * Order (deterministic, see [BookingHistory.STATUS_ORDER]): groups follow a fixed status order;
 * inside a group, open Bookings (Requested, Confirmed) are soonest-first and closed ones
 * most-recent-first, both on `scheduledAt` then id. Booking only holds `serviceId`/`providerId`;
 * the screen resolves titles and names through the existing Service/Provider queries.
 */
class VerHistorialReservasUseCase(
    private val currentUser: ObtenerUsuarioActualUseCase,
    private val repository: BookingRepository,
) {
    suspend operator fun invoke(): DomainResult<BookingHistory> =
        currentUser().flatMap { user ->
            if (user == null || user.isDeleted) return DomainError.Unauthorized.asFailure()
            repository.getBookingsByCustomer(user.id).map { BookingHistory.of(it) }
        }
}
