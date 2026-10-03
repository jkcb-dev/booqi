package corp.khin.solutions.booqi.domain.usecase

import corp.khin.solutions.booqi.domain.model.BookingStatus
import corp.khin.solutions.booqi.domain.model.ProviderReasonCode
import corp.khin.solutions.booqi.domain.model.Reason
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.hours

/**
 * Escenario: "El Proveedor rechaza una solicitud" (docs/domain/provider-flow.md § Grupo 4):
 * a predefined reason or "Otro" with optional free text; the slot is freed (asserted in
 * `ObtenerTimeSlotsDisponiblesUseCaseTest`). Sending the reason to the Customer is deferred.
 */
class RechazarReservaUseCaseTest {

    private val repository = FakeBookingRepository()
    private val clock = FakeClock(REQUESTED_AT + 2.hours)
    private val rechazar = RechazarReservaUseCase(repository, clock)

    @Test
    fun `rejecting with a predefined reason moves the booking to rejected and stores the reason`() = runTest {
        repository.seed(booking())

        val result = rechazar("booking-1", ProviderReasonCode.OUTSIDE_SERVICE_AREA).value()

        assertEquals(BookingStatus.REJECTED, result.status)
        assertEquals(Reason(ProviderReasonCode.OUTSIDE_SERVICE_AREA), result.reason)
        assertEquals(result, repository.stored("booking-1"))
    }

    @Test
    fun `every predefined reason is accepted`() = runTest {
        for (code in ProviderReasonCode.entries) {
            val repo = FakeBookingRepository().also { it.seed(booking()) }

            val result = RechazarReservaUseCase(repo, clock)("booking-1", code).value()

            assertEquals(code, result.reason?.code)
        }
    }

    @Test
    fun `Otro keeps the free text and the text is optional`() = runTest {
        repository.seed(booking())
        repository.seed(booking(id = "booking-2"))

        val withText = rechazar("booking-1", ProviderReasonCode.OTHER, "Me surgió un imprevisto").value()
        val withoutText = rechazar("booking-2", ProviderReasonCode.OTHER).value()

        assertEquals("Me surgió un imprevisto", withText.reason?.note)
        assertNull(withoutText.reason?.note)
    }

    @Test
    fun `rejecting a booking that is not requested is rejected and nothing is written`() = runTest {
        for (status in BookingStatus.entries.filter { it != BookingStatus.REQUESTED }) {
            val repo = FakeBookingRepository().also { it.seed(booking(status = status)) }

            RechazarReservaUseCase(repo, clock)("booking-1", ProviderReasonCode.OTHER).invalidInput()

            assertEquals(0, repo.writeCount, "$status")
        }
    }

    @Test
    fun `rejecting after the window lapsed is rejected`() = runTest {
        repository.seed(booking())
        clock.now = REQUESTED_AT + 25.hours

        rechazar("booking-1", ProviderReasonCode.OTHER).invalidInput()

        assertEquals(BookingStatus.REQUESTED, repository.stored("booking-1").status)
    }

    @Test
    fun `rejecting an unknown booking is NotFound`() = runTest {
        rechazar("missing", ProviderReasonCode.OTHER).assertNotFound()
    }
}
