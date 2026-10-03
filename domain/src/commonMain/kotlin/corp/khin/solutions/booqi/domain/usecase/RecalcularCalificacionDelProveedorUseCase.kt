package corp.khin.solutions.booqi.domain.usecase

import corp.khin.solutions.booqi.core.common.DomainResult
import corp.khin.solutions.booqi.core.common.flatMap
import corp.khin.solutions.booqi.domain.model.ProviderProfile
import corp.khin.solutions.booqi.domain.repository.BookingRepository
import corp.khin.solutions.booqi.domain.repository.ProviderProfileRepository

/**
 * Escenario: "El Cliente califica una cita completada", second half — "se recalcula el promedio
 * general de calificación del Proveedor" (docs/domain/provider-flow.md § Grupo 4).
 *
 * The summary is a *computed aggregate* (docs/DOMAIN.md), never incremental: it is recomputed from
 * **all** of [providerId]'s rated Bookings (mean of the stars, and their count; `null`/0 when
 * there are none) and persisted through [ProviderProfileRepository.updateRating]. The mean is not
 * rounded. [providerId] is the `ProviderProfile.id` (`Booking.providerId` equals it).
 *
 * Idempotent and side-effect-free apart from that write, so it is safe to call again if the write
 * failed after a rating was saved ([CalificarCitaUseCase] surfaces that failure) — the next call,
 * or the next rating, brings the stored summary back in line.
 */
class RecalcularCalificacionDelProveedorUseCase(
    private val bookings: BookingRepository,
    private val profiles: ProviderProfileRepository,
) {
    suspend operator fun invoke(providerId: String): DomainResult<ProviderProfile> =
        bookings.getRatedBookingsByProvider(providerId).flatMap { rated ->
            val stars = rated.mapNotNull { it.rating?.stars }
            profiles.updateRating(
                profileId = providerId,
                ratingAverage = stars.takeIf { it.isNotEmpty() }?.average(),
                ratingCount = stars.size,
            )
        }
}
