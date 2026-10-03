package corp.khin.solutions.booqi.domain.usecase

import corp.khin.solutions.booqi.domain.model.BookingStatus
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.hours

/**
 * Escenario: "El Proveedor acepta una solicitud" (docs/domain/provider-flow.md § Grupo 4) plus the
 * rejected paths: every non-pending status, a lapsed window, an unknown id. The notification
 * ("el Cliente es notificado") is deferred and not asserted.
 */
class AceptarReservaUseCaseTest {

    private val repository = FakeBookingRepository()
    private val clock = FakeClock(REQUESTED_AT + 2.hours)
    private val aceptar = AceptarReservaUseCase(repository, clock)

    @Test
    fun `accepting a requested booking confirms it and persists it`() = runTest {
        repository.seed(booking())

        val result = aceptar("booking-1").value()

        assertEquals(BookingStatus.CONFIRMED, result.status)
        assertEquals(clock.now, result.respondedAt)
        assertEquals(result, repository.stored("booking-1"))
    }

    @Test
    fun `accepting a booking in any state other than requested is rejected and nothing is written`() = runTest {
        for (status in BookingStatus.entries.filter { it != BookingStatus.REQUESTED }) {
            val repo = FakeBookingRepository().also { it.seed(booking(status = status)) }

            AceptarReservaUseCase(repo, clock)("booking-1").invalidInput()

            assertEquals(0, repo.writeCount, "$status")
            assertEquals(status, repo.stored("booking-1").status)
        }
    }

    @Test
    fun `accepting after the 24 hour window lapsed is rejected`() = runTest {
        repository.seed(booking())
        clock.now = REQUESTED_AT + 24.hours

        aceptar("booking-1").invalidInput()

        assertEquals(BookingStatus.REQUESTED, repository.stored("booking-1").status)
        assertEquals(0, repository.writeCount)
    }

    @Test
    fun `accepting an unknown booking is NotFound`() = runTest {
        aceptar("missing").assertNotFound()
    }
}
