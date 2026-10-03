package corp.khin.solutions.booqi.domain.usecase

import corp.khin.solutions.booqi.domain.model.BookingStatus
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.hours

/**
 * Escenario: "El Proveedor marca una cita confirmada como completada" (docs/domain/provider-flow.md
 * § Grupo 4). Manual only; "el Cliente puede dejar una calificación" is asserted in
 * `CalificarCitaUseCaseTest`.
 */
class CompletarCitaUseCaseTest {

    private val repository = FakeBookingRepository()
    private val clock = FakeClock(REQUESTED_AT + 200.hours)
    private val completar = CompletarCitaUseCase(repository, clock)

    @Test
    fun `completing a confirmed booking moves it to completed and persists it`() = runTest {
        repository.seed(booking(status = BookingStatus.CONFIRMED))

        val result = completar("booking-1").value()

        assertEquals(BookingStatus.COMPLETED, result.status)
        assertEquals(clock.now, result.completedAt)
        assertEquals(result, repository.stored("booking-1"))
    }

    @Test
    fun `completing a booking that is not confirmed is rejected and nothing is written`() = runTest {
        for (status in BookingStatus.entries.filter { it != BookingStatus.CONFIRMED }) {
            val repo = FakeBookingRepository().also { it.seed(booking(status = status)) }

            CompletarCitaUseCase(repo, clock)("booking-1").invalidInput()

            assertEquals(0, repo.writeCount, "$status")
            assertEquals(status, repo.stored("booking-1").status)
        }
    }

    @Test
    fun `completing an unknown booking is NotFound`() = runTest {
        completar("missing").assertNotFound()
    }
}
