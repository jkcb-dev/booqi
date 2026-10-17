package corp.khin.solutions.booqi.domain.usecase

import corp.khin.solutions.booqi.domain.model.Booking
import corp.khin.solutions.booqi.domain.model.BookingStatus
import corp.khin.solutions.booqi.domain.model.CustomerReasonCode
import corp.khin.solutions.booqi.domain.model.Reason
import corp.khin.solutions.booqi.domain.model.User
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * Escenarios: "El Cliente cancela una reserva confirmada a tiempo" y "El Cliente intenta cancelar
 * fuera del plazo permitido" (docs/domain/customer-flow.md § Grupo 3), plus ownership, session,
 * the 3-hour boundary and invalid transitions. The freed slot is asserted in
 * `ObtenerTimeSlotsDisponiblesUseCaseTest`; notifying the Provider is deferred.
 */
class CancelarReservaClienteUseCaseTest {

    private val users = FakeUserRepository()
    private val repository = FakeBookingRepository()
    private val clock = FakeClock(REQUESTED_AT)
    private val cancelar = CancelarReservaClienteUseCase(
        ObtenerUsuarioActualUseCase(users),
        repository,
        clock,
        TimeZone.UTC,
    )

    private val me = User("customer-1", "Ana", "ana@example.com", isEmailVerified = true)
    private val start = APPOINTMENT.toInstant(TimeZone.UTC)

    private fun signedIn() = users.seedSignedIn(me)

    private fun confirmed(id: String = "booking-1", customerId: String = me.id): Booking =
        repository.seed(booking(id = id, status = BookingStatus.CONFIRMED, customerId = customerId))

    /** Moves "now" to [before] ahead of the appointment's start. */
    private fun nowIs(before: Duration) {
        clock.now = start - before
    }

    @Test
    fun `cancelling a confirmed booking with more than 3 hours left records the reason`() = runTest {
        signedIn()
        confirmed()
        nowIs(before = 3.hours + 1.minutes)

        val result = cancelar("booking-1", CustomerReasonCode.FOUND_ANOTHER_PROVIDER).value()

        assertEquals(BookingStatus.CANCELLED_BY_CUSTOMER, result.status)
        assertEquals(Reason(CustomerReasonCode.FOUND_ANOTHER_PROVIDER), result.reason)
        assertEquals(result, repository.stored("booking-1"))
    }

    @Test
    fun `each predefined Customer reason is accepted and Otro keeps its optional text`() = runTest {
        signedIn()
        nowIs(before = 1.hours + 3.hours)
        for ((index, code) in CustomerReasonCode.entries.withIndex()) {
            confirmed(id = "b-$index")

            val note = if (code == CustomerReasonCode.OTHER) "Surgió un viaje" else null
            val reason = cancelar("b-$index", code, note).value().reason

            assertEquals(Reason(code, note), reason)
        }
    }

    @Test
    fun `Otro without a note stores none`() = runTest {
        signedIn()
        confirmed()
        nowIs(before = 5.hours)

        assertNull(cancelar("booking-1", CustomerReasonCode.OTHER).value().reason?.note)
    }

    @Test
    fun `exactly 3 hours before the start is still allowed`() = runTest {
        signedIn()
        confirmed()
        nowIs(before = 3.hours)

        assertEquals(BookingStatus.CANCELLED_BY_CUSTOMER, cancelar("booking-1", CustomerReasonCode.CHANGE_OF_PLANS).value().status)
    }

    @Test
    fun `one second inside the 3 hour window the cancellation is refused and nothing is written`() = runTest {
        signedIn()
        confirmed()
        nowIs(before = 3.hours - 1.seconds)

        val message = cancelar("booking-1", CustomerReasonCode.CHANGE_OF_PLANS).invalidInput()

        assertEquals(Booking.TOO_LATE_TO_CANCEL_MESSAGE, message)
        assertEquals(0, repository.writeCount)
        assertEquals(BookingStatus.CONFIRMED, repository.stored("booking-1").status)
    }

    @Test
    fun `cancelling after the appointment has started is refused`() = runTest {
        signedIn()
        confirmed()
        nowIs(before = (-30).minutes)

        assertEquals(Booking.TOO_LATE_TO_CANCEL_MESSAGE, cancelar("booking-1", CustomerReasonCode.OTHER).invalidInput())
    }

    @Test
    fun `a booking that is not confirmed cannot be cancelled by the Customer`() = runTest {
        signedIn()
        nowIs(before = 48.hours)
        for (status in BookingStatus.entries.filter { it != BookingStatus.CONFIRMED }) {
            repository.seed(booking(id = "b-$status", status = status, customerId = me.id))

            cancelar("b-$status", CustomerReasonCode.OTHER).invalidInput()

            assertEquals(status, repository.stored("b-$status").status)
        }
        assertEquals(0, repository.writeCount)
    }

    @Test
    fun `someone else's booking is Unauthorized and stays untouched`() = runTest {
        signedIn()
        confirmed(customerId = "customer-2")
        nowIs(before = 48.hours)

        cancelar("booking-1", CustomerReasonCode.OTHER).assertUnauthorized()

        assertEquals(0, repository.writeCount)
        assertEquals(BookingStatus.CONFIRMED, repository.stored("booking-1").status)
    }

    @Test
    fun `without a session the cancellation is Unauthorized`() = runTest {
        confirmed()
        nowIs(before = 48.hours)

        cancelar("booking-1", CustomerReasonCode.OTHER).assertUnauthorized()

        assertEquals(0, repository.writeCount)
    }

    @Test
    fun `cancelling an unknown booking is NotFound`() = runTest {
        signedIn()

        cancelar("missing", CustomerReasonCode.OTHER).assertNotFound()
    }
}
