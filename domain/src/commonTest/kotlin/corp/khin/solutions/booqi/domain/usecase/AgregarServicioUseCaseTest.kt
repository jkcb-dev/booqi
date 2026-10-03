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
import kotlin.test.assertTrue

/**
 * Escenarios: "El Proveedor agrega un nuevo Servicio" / "El Proveedor agrega un Servicio sin
 * foto" (docs/domain/provider-flow.md § Grupo 2).
 */
class AgregarServicioUseCaseTest {

    private val validDetails = ServiceDetails(
        title = "Manicure gel",
        photoUrl = "https://example.com/gel.jpg",
        description = "Esmaltado semipermanente",
        priceCents = 3500,
        durationMinutes = 60,
        modality = ServiceModality.AMBOS,
    )

    @Test
    fun `adding a service with all fields creates an active service owned by the provider`() = runTest {
        val useCase = AgregarServicioUseCase(FakeServiceRepository())

        val result = useCase("provider-1", validDetails)

        assertIs<DomainResult.Success<Service>>(result)
        assertEquals("provider-1", result.value.providerId)
        assertEquals("Manicure gel", result.value.title)
        assertEquals(3500, result.value.priceCents)
        assertEquals(60, result.value.durationMinutes)
        assertEquals(ServiceModality.AMBOS, result.value.modality)
        assertTrue(result.value.isActive)
    }

    @Test
    fun `adding a service without foto is rejected with a validation error`() = runTest {
        val useCase = AgregarServicioUseCase(FakeServiceRepository())

        val result = useCase("provider-1", validDetails.copy(photoUrl = ""))

        assertIs<DomainResult.Failure>(result)
        val error = result.error
        assertIs<DomainError.InvalidInput>(error)
        assertEquals("La foto es obligatoria", error.message)
    }

    @Test
    fun `adding a service with blank whitespace foto is also rejected`() = runTest {
        val useCase = AgregarServicioUseCase(FakeServiceRepository())

        val result = useCase("provider-1", validDetails.copy(photoUrl = "   "))

        assertIs<DomainResult.Failure>(result)
        assertIs<DomainError.InvalidInput>(result.error)
    }
}
