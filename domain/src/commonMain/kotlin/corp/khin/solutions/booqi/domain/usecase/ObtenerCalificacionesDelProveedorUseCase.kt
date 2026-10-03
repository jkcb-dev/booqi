package corp.khin.solutions.booqi.domain.usecase

import corp.khin.solutions.booqi.core.common.DomainResult
import corp.khin.solutions.booqi.domain.model.Booking
import corp.khin.solutions.booqi.domain.repository.BookingRepository

/**
 * Query behind the reviews on the Provider's public profile (Figma P11, and the Customer's C4):
 * the Bookings of [providerId] that carry a [corp.khin.solutions.booqi.domain.model.Rating]
 * (stars + comment in `Booking.rating`), newest first by completion date. The overall average and
 * count come from `ProviderProfile.ratingAverage`/`ratingCount`, not from this list. No ratings
 * yields an empty list, not an error. Reviewer names need the Identity context (not built).
 */
class ObtenerCalificacionesDelProveedorUseCase(
    private val repository: BookingRepository,
) {
    suspend operator fun invoke(providerId: String): DomainResult<List<Booking>> =
        repository.getRatedBookingsByProvider(providerId)
}
