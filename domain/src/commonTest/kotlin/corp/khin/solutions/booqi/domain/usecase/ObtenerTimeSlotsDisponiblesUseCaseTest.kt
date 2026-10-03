package corp.khin.solutions.booqi.domain.usecase

import corp.khin.solutions.booqi.core.common.DomainError
import corp.khin.solutions.booqi.core.common.DomainResult
import corp.khin.solutions.booqi.domain.model.BookingStatus
import corp.khin.solutions.booqi.domain.model.DateRange
import corp.khin.solutions.booqi.domain.model.ProviderReasonCode
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.time.Duration.Companion.hours

/**
 * Escenarios (docs/domain/provider-flow.md § Grupo 4): "el TimeSlot deja de estar disponible para
 * otros Clientes mientras la solicitud está pendiente" and "el TimeSlot vuelve a estar disponible"
 * after a rejection, an expiry or a cancellation — plus overlap by interval, pause and the
 * honored-accepted-booking rule.
 */
class ObtenerTimeSlotsDisponiblesUseCaseTest {

    private val f = SchedulingFixture()
    private val monday = DateRange(MONDAY_DATE, MONDAY_DATE)

    private suspend fun starts(duration: Int = 60) =
        f.availableSlots(f.providerId, duration, monday).value().map { it.start }

    private fun bookAt(hour: Int, status: BookingStatus, id: String = "booking-$hour", minutes: Int = 60) =
        f.bookings.seed(
            booking(
                id = id,
                status = status,
                providerId = f.providerId,
                scheduledAt = LocalDateTime(MONDAY_DATE, time(hour)),
                durationMinutes = minutes,
            ),
        )

    @Test
    fun `with no bookings every generated slot is available`() = runTest {
        f.givenProvider()

        assertEquals(listOf(time(9), time(10), time(11)), starts())
    }

    @Test
    fun `a requested booking takes its slot away while pending`() = runTest {
        f.givenProvider()
        bookAt(10, BookingStatus.REQUESTED)

        assertEquals(listOf(time(9), time(11)), starts())
    }

    @Test
    fun `a confirmed booking takes its slot away`() = runTest {
        f.givenProvider()
        bookAt(10, BookingStatus.CONFIRMED)

        assertEquals(listOf(time(9), time(11)), starts())
    }

    @Test
    fun `rejected expired cancelled and completed bookings leave the slot available`() = runTest {
        f.givenProvider()
        val freeing = BookingStatus.entries.filterNot { it.occupiesSlot }
        freeing.forEachIndexed { index, status -> bookAt(10, status, id = "b-$index") }

        assertEquals(listOf(time(9), time(10), time(11)), starts())
    }

    @Test
    fun `a slot comes back after the provider rejects the pending request`() = runTest {
        f.givenProvider()
        bookAt(10, BookingStatus.REQUESTED, id = "booking-1")
        assertEquals(listOf(time(9), time(11)), starts())

        RechazarReservaUseCase(f.bookings, f.clock)("booking-1", ProviderReasonCode.NOT_AVAILABLE_AT_THIS_TIME).value()

        assertEquals(listOf(time(9), time(10), time(11)), starts())
    }

    @Test
    fun `a slot comes back after the request expires`() = runTest {
        f.givenProvider()
        bookAt(10, BookingStatus.REQUESTED)
        f.clock.now = REQUESTED_AT + 24.hours

        ExpirarSolicitudesVencidasUseCase(f.bookings, f.clock)().value()

        assertEquals(listOf(time(9), time(10), time(11)), starts())
    }

    @Test
    fun `a slot comes back after the provider cancels the accepted appointment`() = runTest {
        f.givenProvider()
        bookAt(10, BookingStatus.CONFIRMED, id = "booking-1")
        assertEquals(listOf(time(9), time(11)), starts())

        CancelarReservaAceptadaUseCase(f.bookings)("booking-1", ProviderReasonCode.OTHER).value()

        assertEquals(listOf(time(9), time(10), time(11)), starts())
    }

    @Test
    fun `slots overlap by interval not by start time`() = runTest {
        f.givenProvider(startHour = 9, endHour = 12)
        bookAt(10, BookingStatus.CONFIRMED, minutes = 60)

        // A 30-minute service: 10:00 and 10:30 sit inside the 60-minute booking; back-to-back stays free.
        assertEquals(
            listOf(time(9), time(9, 30), time(11), time(11, 30)),
            starts(duration = 30),
        )
    }

    @Test
    fun `a longer service loses the slots that would run into an existing booking`() = runTest {
        f.givenProvider(startHour = 9, endHour = 12)
        bookAt(11, BookingStatus.REQUESTED, minutes = 30)

        // 90-minute service: 09:00-10:30 fits; 10:30-12:00 runs into the 11:00 booking.
        assertEquals(listOf(time(9)), starts(duration = 90))
    }

    @Test
    fun `another provider's bookings do not affect this provider`() = runTest {
        f.givenProvider()
        f.bookings.seed(
            booking(providerId = "someone-else", scheduledAt = LocalDateTime(MONDAY_DATE, time(10))),
        )

        assertEquals(listOf(time(9), time(10), time(11)), starts())
    }

    @Test
    fun `an accepted booking outside the current weekly hours changes nothing and is not an error`() = runTest {
        f.givenProvider(startHour = 9, endHour = 12)
        bookAt(15, BookingStatus.CONFIRMED)

        assertEquals(listOf(time(9), time(10), time(11)), starts())
    }

    @Test
    fun `a paused provider has no slots during the pause`() = runTest {
        f.givenProvider()
        f.profiles.setPausedRange(f.providerId, DateRange(MONDAY_DATE, MONDAY_DATE))

        assertEquals(emptyList(), starts())
    }

    @Test
    fun `a missing provider profile is NotFound`() = runTest {
        f.availableSlots("ghost", 60, monday).assertNotFound()
    }

    @Test
    fun `a non positive duration is rejected as invalid input`() = runTest {
        f.givenProvider()

        val result = f.availableSlots(f.providerId, 0, monday)

        assertIs<DomainResult.Failure>(result)
        assertIs<DomainError.InvalidInput>(result.error)
    }
}
