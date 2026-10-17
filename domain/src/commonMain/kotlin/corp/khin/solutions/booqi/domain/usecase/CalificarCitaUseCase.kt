package corp.khin.solutions.booqi.domain.usecase

import corp.khin.solutions.booqi.core.common.DomainError
import corp.khin.solutions.booqi.core.common.DomainResult
import corp.khin.solutions.booqi.core.common.asFailure
import corp.khin.solutions.booqi.core.common.flatMap
import corp.khin.solutions.booqi.core.common.map
import corp.khin.solutions.booqi.domain.model.Booking
import corp.khin.solutions.booqi.domain.model.Rating
import corp.khin.solutions.booqi.domain.repository.BookingRepository

/**
 * Escenarios: "El Cliente califica una cita completada" / "El Cliente intenta calificar una cita
 * no completada" (docs/domain/provider-flow.md § Grupo 4). Shared with the Customer side (#25).
 *
 * Checked in this order, nothing persisted on any failure (#62, same pattern as
 * [CancelarReservaClienteUseCase]):
 * 1. a signed-in user — else `Unauthorized`;
 * 2. the Booking exists — else `NotFound`;
 * 3. it is that user's own (`customerId == user.id`) — else `Unauthorized`. Ownership is checked
 *    before the business rules so that a stranger never learns the Booking's state (completed,
 *    already rated) from a more specific `InvalidInput`;
 * 4. [Booking.rate], below.
 *
 * [Booking.rate] enforces the rules in one place: status must be COMPLETED (a REQUESTED/CONFIRMED
 * one is rejected with a message saying only completed appointments can be rated), the Booking
 * must not already have a rating, and [stars] must be within 1..5 — each an
 * [corp.khin.solutions.booqi.core.common.DomainError.InvalidInput] with nothing persisted. A
 * blank [comment] is stored as none.
 *
 * After saving, the Provider's average rating is recomputed from all their rated Bookings by
 * [RecalcularCalificacionDelProveedorUseCase] — kept out of the Booking aggregate. If that second
 * write fails the rating itself *is* already saved and the failure is returned; re-running the
 * recalculation (or the next rating) repairs the stored summary.
 */
class CalificarCitaUseCase(
    private val currentUser: ObtenerUsuarioActualUseCase,
    private val repository: BookingRepository,
    private val recalculateRating: RecalcularCalificacionDelProveedorUseCase,
) {
    suspend operator fun invoke(bookingId: String, stars: Int, comment: String? = null): DomainResult<Booking> =
        currentUser().flatMap { user ->
            if (user == null || user.isDeleted) return DomainError.Unauthorized.asFailure()
            repository.getBooking(bookingId).flatMap { booking ->
                if (booking.customerId != user.id) return DomainError.Unauthorized.asFailure()
                booking.rate(Rating(stars, comment))
                    .flatMap { repository.updateBooking(it) }
                    .flatMap { rated -> recalculateRating(rated.providerId).map { rated } }
            }
        }
}
