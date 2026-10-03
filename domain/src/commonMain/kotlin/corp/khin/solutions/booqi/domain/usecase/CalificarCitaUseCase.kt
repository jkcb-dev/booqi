package corp.khin.solutions.booqi.domain.usecase

import corp.khin.solutions.booqi.core.common.DomainResult
import corp.khin.solutions.booqi.core.common.flatMap
import corp.khin.solutions.booqi.core.common.map
import corp.khin.solutions.booqi.domain.model.Booking
import corp.khin.solutions.booqi.domain.model.Rating
import corp.khin.solutions.booqi.domain.repository.BookingRepository

/**
 * Escenarios: "El Cliente califica una cita completada" / "El Cliente intenta calificar una cita
 * no completada" (docs/domain/provider-flow.md § Grupo 4). Shared with the Customer side (#25).
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
    private val repository: BookingRepository,
    private val recalculateRating: RecalcularCalificacionDelProveedorUseCase,
) {
    suspend operator fun invoke(bookingId: String, stars: Int, comment: String? = null): DomainResult<Booking> =
        repository.getBooking(bookingId)
            .flatMap { it.rate(Rating(stars, comment)) }
            .flatMap { repository.updateBooking(it) }
            .flatMap { rated -> recalculateRating(rated.providerId).map { rated } }
}
