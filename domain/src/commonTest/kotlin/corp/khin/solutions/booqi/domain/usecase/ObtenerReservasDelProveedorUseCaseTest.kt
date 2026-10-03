package corp.khin.solutions.booqi.domain.usecase

import corp.khin.solutions.booqi.domain.model.BookingStatus
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals

/** Query behind the Provider's agenda (Figma P10): by status filter, next appointment first. */
class ObtenerReservasDelProveedorUseCaseTest {

    private val repository = FakeBookingRepository()
    private val obtener = ObtenerReservasDelProveedorUseCase(repository)

    private fun seed(id: String, status: BookingStatus, day: Int, hour: Int = 10, providerId: String = "provider-1") =
        repository.seed(
            booking(id = id, status = status, providerId = providerId, scheduledAt = LocalDateTime(2026, 10, day, hour, 0)),
        )

    @Test
    fun `without a filter it returns every status ordered by scheduled time then id`() = runTest {
        seed("late", BookingStatus.COMPLETED, day = 20)
        seed("b", BookingStatus.CONFIRMED, day = 12)
        seed("a", BookingStatus.REQUESTED, day = 12)
        seed("early", BookingStatus.REJECTED, day = 8, hour = 9)
        seed("other", BookingStatus.CONFIRMED, day = 12, providerId = "provider-2")

        assertEquals(listOf("early", "a", "b", "late"), obtener("provider-1").value().map { it.id })
    }

    @Test
    fun `a status filter keeps only those statuses`() = runTest {
        seed("confirmed-2", BookingStatus.CONFIRMED, day = 14)
        seed("confirmed-1", BookingStatus.CONFIRMED, day = 12)
        seed("requested", BookingStatus.REQUESTED, day = 13)
        seed("done", BookingStatus.COMPLETED, day = 5)

        assertEquals(
            listOf("confirmed-1", "confirmed-2"),
            obtener("provider-1", setOf(BookingStatus.CONFIRMED)).value().map { it.id },
        )
        assertEquals(
            listOf("done", "confirmed-1", "requested", "confirmed-2"),
            obtener("provider-1", setOf(BookingStatus.CONFIRMED, BookingStatus.REQUESTED, BookingStatus.COMPLETED))
                .value().map { it.id },
        )
    }

    @Test
    fun `an empty filter matches nothing and a provider with no bookings gets an empty list`() = runTest {
        seed("a", BookingStatus.CONFIRMED, day = 12)

        assertEquals(emptyList(), obtener("provider-1", emptySet()).value())
        assertEquals(emptyList(), obtener("nobody").value())
    }
}
