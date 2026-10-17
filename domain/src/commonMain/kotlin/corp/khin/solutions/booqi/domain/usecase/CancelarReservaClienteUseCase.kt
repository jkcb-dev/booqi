package corp.khin.solutions.booqi.domain.usecase

import corp.khin.solutions.booqi.core.common.DomainError
import corp.khin.solutions.booqi.core.common.DomainResult
import corp.khin.solutions.booqi.core.common.asFailure
import corp.khin.solutions.booqi.core.common.flatMap
import corp.khin.solutions.booqi.domain.model.Booking
import corp.khin.solutions.booqi.domain.model.CustomerReasonCode
import corp.khin.solutions.booqi.domain.repository.BookingRepository
import kotlin.time.Clock
import kotlinx.datetime.TimeZone

/**
 * Escenarios: "El Cliente cancela una reserva confirmada a tiempo" / "El Cliente intenta cancelar
 * fuera del plazo permitido" (docs/domain/customer-flow.md § Grupo 3): CONFIRMED →
 * CANCELLED_BY_CUSTOMER with a predefined [CustomerReasonCode] (a different list from the
 * Provider's; "Otro" + optional free-text [note]). The TimeSlot is freed implicitly
 * ([corp.khin.solutions.booqi.domain.model.BookingStatus.occupiesSlot]).
 *
 * Checked in this order, nothing persisted on any failure:
 * 1. a signed-in user — else `Unauthorized`;
 * 2. the Booking exists — else `NotFound`;
 * 3. it is that user's own (`customerId == user.id`) — else `Unauthorized` (a Customer can't touch
 *    someone else's Booking, nor learn anything about it beyond that it exists);
 * 4. [Booking.cancelByCustomer]: only a CONFIRMED Booking (else `InvalidInput`, invalid transition),
 *    and only up to 3 hours before the start, **inclusive** (`now <= start - 3h`; exactly three
 *    hours left is still allowed, "hasta 3 horas antes") — else `InvalidInput` with
 *    [Booking.TOO_LATE_TO_CANCEL_MESSAGE].
 *
 * [clock] and [timeZone] are injected so tests fix "now"; `scheduledAt` is the Provider's wall-clock
 * start read in [timeZone] (the device's by default, as everywhere else — there is no timezone
 * feature yet). Notifying the Provider of the cancellation and reason is deferred (no notification
 * system yet).
 */
class CancelarReservaClienteUseCase(
    private val currentUser: ObtenerUsuarioActualUseCase,
    private val repository: BookingRepository,
    private val clock: Clock = Clock.System,
    private val timeZone: TimeZone = TimeZone.currentSystemDefault(),
) {
    suspend operator fun invoke(
        bookingId: String,
        reason: CustomerReasonCode,
        note: String? = null,
    ): DomainResult<Booking> =
        currentUser().flatMap { user ->
            if (user == null || user.isDeleted) return DomainError.Unauthorized.asFailure()
            repository.getBooking(bookingId).flatMap { booking ->
                if (booking.customerId != user.id) return DomainError.Unauthorized.asFailure()
                booking.cancelByCustomer(reason, note, clock.now(), timeZone)
                    .flatMap { repository.updateBooking(it) }
            }
        }
}
