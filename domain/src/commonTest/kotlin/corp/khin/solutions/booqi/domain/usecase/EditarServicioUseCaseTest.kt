package corp.khin.solutions.booqi.domain.usecase

import corp.khin.solutions.booqi.core.common.DomainError
import corp.khin.solutions.booqi.core.common.DomainResult
import corp.khin.solutions.booqi.domain.model.Service
import corp.khin.solutions.booqi.domain.model.ServiceDetails
import corp.khin.solutions.booqi.domain.model.ServiceModality
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

/**
 * Escenario: "El Proveedor edita un Servicio existente" (docs/domain/provider-flow.md § Grupo 2).
 * The "citas ya reservadas conservan el precio/duración" clause is `Booking`'s snapshot concern
 * (future ticket) and is intentionally not tested here — see [EditarServicioUseCase] KDoc.
 */
class EditarServicioUseCaseTest {

    private val originalDetails = ServiceDetails(
        title = "Manicure gel",
        photoUrl = "https://example.com/gel.jpg",
        description = "Esmaltado semipermanente",
        priceCents = 3500,
        durationMinutes = 60,
        modality = ServiceModality.LOCAL,
    )

    @Test
    fun `editing price and duration updates the service going forward`() = runTest {
        val repository = FakeServiceRepository()
        val published = publish(repository)
        val useCase = EditarServicioUseCase(repository)

        val result = useCase(
            published.id,
            originalDetails.copy(priceCents = 4500, durationMinutes = 90),
        )

        assertIs<DomainResult.Success<Service>>(result)
        assertEquals(4500, result.value.priceCents)
        assertEquals(90, result.value.durationMinutes)
        assertEquals(published.id, result.value.id)
        assertEquals(published.providerId, result.value.providerId)
        assertEquals(true, result.value.isActive)
    }

    @Test
    fun `editing a service with a blank foto is rejected`() = runTest {
        val repository = FakeServiceRepository()
        val published = publish(repository)
        val useCase = EditarServicioUseCase(repository)

        val result = useCase(published.id, originalDetails.copy(photoUrl = " "))

        assertIs<DomainResult.Failure>(result)
        assertIs<DomainError.InvalidInput>(result.error)
    }

    @Test
    fun `editing an unknown service propagates NotFound`() = runTest {
        val useCase = EditarServicioUseCase(FakeServiceRepository())

        val result = useCase("missing", originalDetails)

        assertIs<DomainResult.Failure>(result)
        assertEquals(DomainError.NotFound, result.error)
    }

    private suspend fun publish(repository: FakeServiceRepository): Service {
        val result = AgregarServicioUseCase(repository)("provider-1", originalDetails)
        return (result as DomainResult.Success<Service>).value
    }
}
