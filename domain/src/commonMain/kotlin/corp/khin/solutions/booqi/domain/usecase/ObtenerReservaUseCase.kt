package corp.khin.solutions.booqi.domain.usecase

import corp.khin.solutions.booqi.core.common.DomainResult
import corp.khin.solutions.booqi.domain.model.Booking
import corp.khin.solutions.booqi.domain.repository.BookingRepository

/**
 * Query behind a single Booking's detail (Figma P9 "Detalle solicitud", P10 "Cita confirmada"):
 * the Booking identified by [bookingId] in any status, or `NotFound`. It carries the snapshots
 * (price, duration, delivery address), the Customer's note, the reason (if rejected/cancelled) and
 * the rating — the screen resolves the Service title via [ObtenerServicioUseCase] (the Booking only
 * holds `serviceId`). The Customer's display name needs the Identity context (not built).
 */
class ObtenerReservaUseCase(
    private val repository: BookingRepository,
) {
    suspend operator fun invoke(bookingId: String): DomainResult<Booking> =
        repository.getBooking(bookingId)
}
