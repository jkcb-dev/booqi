package corp.khin.solutions.booqi.domain.usecase

import corp.khin.solutions.booqi.domain.model.BookingStatus
import corp.khin.solutions.booqi.domain.model.ProviderReasonCode
import corp.khin.solutions.booqi.domain.model.Reason
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

/** Query behind a Booking's detail (Figma P9/P10): any status, with its reason, or NotFound. */
class ObtenerReservaUseCaseTest {

    private val repository = FakeBookingRepository()
    private val obtener = ObtenerReservaUseCase(repository)

    @Test
    fun `it returns the booking whatever its status`() = runTest {
        for (status in BookingStatus.entries) {
            val seeded = repository.seed(booking(id = "b-$status", status = status))

            assertEquals(seeded, obtener("b-$status").value())
        }
    }

    @Test
    fun `it carries the rejection reason`() = runTest {
        val reason = Reason(ProviderReasonCode.OTHER, "Imprevisto")
        repository.seed(booking(status = BookingStatus.REJECTED, reason = reason))

        assertEquals(reason, obtener("booking-1").value().reason)
    }

    @Test
    fun `an unknown booking is NotFound`() = runTest {
        obtener("missing").assertNotFound()
    }
}
