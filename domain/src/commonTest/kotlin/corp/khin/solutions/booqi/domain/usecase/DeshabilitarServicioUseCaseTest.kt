package corp.khin.solutions.booqi.domain.usecase

import corp.khin.solutions.booqi.core.common.DomainError
import corp.khin.solutions.booqi.core.common.DomainResult
import corp.khin.solutions.booqi.domain.model.Service
import corp.khin.solutions.booqi.domain.model.ServiceDetails
import corp.khin.solutions.booqi.domain.model.ServiceModality
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs

/**
 * Escenario: "El Proveedor deshabilita un Servicio" (docs/domain/provider-flow.md § Grupo 2).
 * Soft-delete: the service keeps its id/fields (so `Booking.serviceId` never dangles) and only
 * flips [Service.isActive]; the use case has no Booking dependency, so it cannot cancel bookings.
 */
class DeshabilitarServicioUseCaseTest {

    @Test
    fun `disabling a service sets isActive to false and keeps the rest intact`() = runTest {
        val repository = FakeServiceRepository()
        val added = AgregarServicioUseCase(repository)(
            "provider-1",
            ServiceDetails(
                title = "Manicure gel",
                photoUrl = "https://example.com/gel.jpg",
                description = "Esmaltado semipermanente",
                priceCents = 3500,
                durationMinutes = 60,
                modality = ServiceModality.DOMICILIO,
            ),
        ) as DomainResult.Success<Service>

        val result = DeshabilitarServicioUseCase(repository)(added.value.id)

        assertIs<DomainResult.Success<Service>>(result)
        assertFalse(result.value.isActive)
        assertEquals(added.value.copy(isActive = false), result.value)
    }

    @Test
    fun `disabling an unknown service propagates NotFound`() = runTest {
        val result = DeshabilitarServicioUseCase(FakeServiceRepository())("missing")

        assertIs<DomainResult.Failure>(result)
        assertEquals(DomainError.NotFound, result.error)
    }
}
