package corp.khin.solutions.booqi.domain.usecase

import corp.khin.solutions.booqi.domain.model.BookingStatus
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds

/**
 * Query behind the Provider's request inbox (Figma P8): pending requests the Provider can still act
 * on, oldest request first.
 */
class ObtenerSolicitudesPendientesUseCaseTest {

    private val repository = FakeBookingRepository()
    private val clock = FakeClock(REQUESTED_AT + 5.hours)
    private val pendientes = ObtenerSolicitudesPendientesUseCase(repository, clock)

    @Test
    fun `it lists only requested bookings of the provider`() = runTest {
        repository.seed(booking(id = "pending"))
        repository.seed(booking(id = "confirmed", status = BookingStatus.CONFIRMED))
        repository.seed(booking(id = "rejected", status = BookingStatus.REJECTED))
        repository.seed(booking(id = "other", providerId = "provider-2"))

        assertEquals(listOf("pending"), pendientes("provider-1").value().map { it.id })
    }

    @Test
    fun `the oldest request comes first and ties break by id`() = runTest {
        repository.seed(booking(id = "c", requestedAt = REQUESTED_AT + 3.hours))
        repository.seed(booking(id = "b", requestedAt = REQUESTED_AT + 1.hours))
        repository.seed(booking(id = "a", requestedAt = REQUESTED_AT + 1.hours))

        assertEquals(listOf("a", "b", "c"), pendientes("provider-1").value().map { it.id })
    }

    @Test
    fun `a request past its 24 hour window is left out even before it is swept to expired`() = runTest {
        repository.seed(booking(id = "lapsed"))
        repository.seed(booking(id = "alive", requestedAt = REQUESTED_AT + 1.hours))
        clock.now = REQUESTED_AT + 24.hours

        assertEquals(listOf("alive"), pendientes("provider-1").value().map { it.id })

        clock.now = REQUESTED_AT + 24.hours - 1.milliseconds
        assertEquals(listOf("alive", "lapsed"), pendientes("provider-1").value().map { it.id }.sorted())
    }

    @Test
    fun `a provider with no requests gets an empty list`() = runTest {
        assertEquals(emptyList(), pendientes("provider-1").value())
    }
}
