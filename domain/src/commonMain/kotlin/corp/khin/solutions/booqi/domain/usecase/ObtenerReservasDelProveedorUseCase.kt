package corp.khin.solutions.booqi.domain.usecase

import corp.khin.solutions.booqi.core.common.DomainResult
import corp.khin.solutions.booqi.domain.model.Booking
import corp.khin.solutions.booqi.domain.model.BookingStatus
import corp.khin.solutions.booqi.domain.repository.BookingRepository

/**
 * Query behind the Provider's agenda (Figma P10 lists the CONFIRMED appointments): the Bookings of
 * [providerId] whose status is in [statuses], or **all** of them when [statuses] is `null`.
 *
 * Order: by scheduled date/time ascending (then id), i.e. the next appointment first. An empty
 * [statuses] set matches nothing. No matches yields an empty list, not an error. The pending inbox
 * has its own use case ([ObtenerSolicitudesPendientesUseCase]), which orders by request age.
 */
class ObtenerReservasDelProveedorUseCase(
    private val repository: BookingRepository,
) {
    suspend operator fun invoke(
        providerId: String,
        statuses: Set<BookingStatus>? = null,
    ): DomainResult<List<Booking>> = repository.getBookingsByProvider(providerId, statuses)
}
