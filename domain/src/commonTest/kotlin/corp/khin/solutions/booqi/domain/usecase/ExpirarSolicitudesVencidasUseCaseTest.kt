package corp.khin.solutions.booqi.domain.usecase

import corp.khin.solutions.booqi.domain.model.BookingStatus
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds

/**
 * Escenario: "Una solicitud expira sin respuesta" (docs/domain/provider-flow.md § Grupo 4), with a
 * fake clock around the 24h boundary: `requestedAt + 24h <= now` expires, one millisecond earlier
 * does not. The slot being freed is asserted in `ObtenerTimeSlotsDisponiblesUseCaseTest`.
 */
class ExpirarSolicitudesVencidasUseCaseTest {

    private val repository = FakeBookingRepository()
    private val clock = FakeClock(REQUESTED_AT)
    private val expirar = ExpirarSolicitudesVencidasUseCase(repository, clock)

    @Test
    fun `a request pending for more than 24 hours expires`() = runTest {
        repository.seed(booking())
        clock.now = REQUESTED_AT + 24.hours + 1.milliseconds

        assertEquals(1, expirar().value())

        assertEquals(BookingStatus.EXPIRED, repository.stored("booking-1").status)
    }

    @Test
    fun `a request pending for exactly 24 hours expires`() = runTest {
        repository.seed(booking())
        clock.now = REQUESTED_AT + 24.hours

        assertEquals(1, expirar().value())

        assertEquals(BookingStatus.EXPIRED, repository.stored("booking-1").status)
    }

    @Test
    fun `a request one millisecond short of 24 hours does not expire`() = runTest {
        repository.seed(booking())
        clock.now = REQUESTED_AT + 24.hours - 1.milliseconds

        assertEquals(0, expirar().value())

        assertEquals(BookingStatus.REQUESTED, repository.stored("booking-1").status)
        assertEquals(0, repository.writeCount)
    }

    @Test
    fun `only the overdue requests expire and the count says how many`() = runTest {
        repository.seed(booking(id = "old-1", requestedAt = REQUESTED_AT))
        repository.seed(booking(id = "old-2", providerId = "provider-2", requestedAt = REQUESTED_AT + 1.hours))
        repository.seed(booking(id = "fresh", requestedAt = REQUESTED_AT + 20.hours))
        clock.now = REQUESTED_AT + 26.hours

        assertEquals(2, expirar().value())

        assertEquals(BookingStatus.EXPIRED, repository.stored("old-1").status)
        assertEquals(BookingStatus.EXPIRED, repository.stored("old-2").status)
        assertEquals(BookingStatus.REQUESTED, repository.stored("fresh").status)
    }

    @Test
    fun `bookings that are not requested are never touched however old`() = runTest {
        for ((index, status) in BookingStatus.entries.filter { it != BookingStatus.REQUESTED }.withIndex()) {
            repository.seed(booking(id = "b-$index", status = status))
        }
        clock.now = REQUESTED_AT + 1000.hours

        assertEquals(0, expirar().value())
        assertEquals(0, repository.writeCount)
    }

    @Test
    fun `running it twice expires nothing the second time`() = runTest {
        repository.seed(booking())
        clock.now = REQUESTED_AT + 30.hours

        assertEquals(1, expirar().value())
        assertEquals(0, expirar().value())
    }

    @Test
    fun `with nothing pending it returns zero`() = runTest {
        assertEquals(0, expirar().value())
    }
}
