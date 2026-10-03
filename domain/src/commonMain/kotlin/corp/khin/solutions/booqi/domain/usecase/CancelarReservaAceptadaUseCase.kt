package corp.khin.solutions.booqi.domain.usecase

import corp.khin.solutions.booqi.core.common.DomainResult
import corp.khin.solutions.booqi.core.common.flatMap
import corp.khin.solutions.booqi.domain.model.Booking
import corp.khin.solutions.booqi.domain.model.ProviderReasonCode
import corp.khin.solutions.booqi.domain.repository.BookingRepository

/**
 * Escenario: "El Proveedor cancela una cita ya confirmada" (docs/domain/provider-flow.md § Grupo
 * 4): CONFIRMED → CANCELLED_BY_PROVIDER with a predefined [ProviderReasonCode] (the same list as
 * rejecting; "Otro" + optional free-text [note]). The TimeSlot is freed implicitly. No time limit
 * applies to the Provider (the 3-hour rule is the Customer's, #25).
 *
 * Transition rules live in [Booking.cancelByProvider]; an invalid one is an `InvalidInput` and
 * nothing is persisted. Notifying the Customer is deferred (no notification system yet).
 */
class CancelarReservaAceptadaUseCase(
    private val repository: BookingRepository,
) {
    suspend operator fun invoke(
        bookingId: String,
        reason: ProviderReasonCode,
        note: String? = null,
    ): DomainResult<Booking> =
        repository.getBooking(bookingId)
            .flatMap { it.cancelByProvider(reason, note) }
            .flatMap { repository.updateBooking(it) }
}
