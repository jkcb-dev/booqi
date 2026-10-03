package corp.khin.solutions.booqi.domain.usecase

import corp.khin.solutions.booqi.core.common.DomainResult
import corp.khin.solutions.booqi.core.common.flatMap
import corp.khin.solutions.booqi.domain.model.Booking
import corp.khin.solutions.booqi.domain.model.ProviderReasonCode
import corp.khin.solutions.booqi.domain.repository.BookingRepository
import kotlin.time.Clock

/**
 * Escenario: "El Proveedor rechaza una solicitud" (docs/domain/provider-flow.md § Grupo 4):
 * REQUESTED → REJECTED with a predefined [ProviderReasonCode] — "Otro" with an optional free-text
 * [note] (a blank note is stored as none). The TimeSlot is freed implicitly: a REJECTED Booking no
 * longer [corp.khin.solutions.booqi.domain.model.BookingStatus.occupiesSlot].
 *
 * Transition rules live in [Booking.reject]; an invalid one is an `InvalidInput` and nothing is
 * persisted. Sending the reason to the Customer is deferred (no notification system yet).
 */
class RechazarReservaUseCase(
    private val repository: BookingRepository,
    private val clock: Clock = Clock.System,
) {
    suspend operator fun invoke(
        bookingId: String,
        reason: ProviderReasonCode,
        note: String? = null,
    ): DomainResult<Booking> =
        repository.getBooking(bookingId)
            .flatMap { it.reject(reason, note, clock.now()) }
            .flatMap { repository.updateBooking(it) }
}
