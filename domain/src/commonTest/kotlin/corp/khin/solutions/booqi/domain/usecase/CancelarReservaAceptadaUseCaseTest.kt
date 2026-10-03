package corp.khin.solutions.booqi.domain.usecase

import corp.khin.solutions.booqi.domain.model.BookingStatus
import corp.khin.solutions.booqi.domain.model.ProviderReasonCode
import corp.khin.solutions.booqi.domain.model.Reason
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Escenario: "El Proveedor cancela una cita ya confirmada" (docs/domain/provider-flow.md § Grupo 4):
 * predefined reason or "Otro"; the slot is freed (asserted in `ObtenerTimeSlotsDisponiblesUseCaseTest`).
 * Notifying the Customer is deferred.
 */
class CancelarReservaAceptadaUseCaseTest {

    private val repository = FakeBookingRepository()
    private val cancelar = CancelarReservaAceptadaUseCase(repository)

    @Test
    fun `cancelling a confirmed booking moves it to cancelled by provider with the reason`() = runTest {
        repository.seed(booking(status = BookingStatus.CONFIRMED))

        val result = cancelar("booking-1", ProviderReasonCode.SERVICE_TEMPORARILY_UNAVAILABLE).value()

        assertEquals(BookingStatus.CANCELLED_BY_PROVIDER, result.status)
        assertEquals(Reason(ProviderReasonCode.SERVICE_TEMPORARILY_UNAVAILABLE), result.reason)
        assertEquals(result, repository.stored("booking-1"))
    }

    @Test
    fun `Otro keeps the optional free text`() = runTest {
        repository.seed(booking(status = BookingStatus.CONFIRMED))
        repository.seed(booking(id = "booking-2", status = BookingStatus.CONFIRMED))

        assertEquals("Emergencia", cancelar("booking-1", ProviderReasonCode.OTHER, "Emergencia").value().reason?.note)
        assertNull(cancelar("booking-2", ProviderReasonCode.OTHER).value().reason?.note)
    }

    @Test
    fun `cancelling a booking that is not confirmed is rejected and nothing is written`() = runTest {
        for (status in BookingStatus.entries.filter { it != BookingStatus.CONFIRMED }) {
            val repo = FakeBookingRepository().also { it.seed(booking(status = status)) }

            CancelarReservaAceptadaUseCase(repo)("booking-1", ProviderReasonCode.OTHER).invalidInput()

            assertEquals(0, repo.writeCount, "$status")
            assertEquals(status, repo.stored("booking-1").status)
        }
    }

    @Test
    fun `cancelling an unknown booking is NotFound`() = runTest {
        cancelar("missing", ProviderReasonCode.OTHER).assertNotFound()
    }
}
