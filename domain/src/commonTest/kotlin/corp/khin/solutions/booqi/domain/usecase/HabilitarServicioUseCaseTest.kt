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
import kotlin.test.assertTrue

/**
 * Escenarios: "El Proveedor re-habilita un Servicio deshabilitado" / "El Proveedor habilita un
 * Servicio que ya estaba activo" / "El Proveedor consulta o habilita un Servicio que no existe"
 * (docs/domain/provider-flow.md § Grupo 2). Re-enabling is the exact inverse of disabling: only
 * [Service.isActive] flips, so the service is eligible for Customer search again.
 */
class HabilitarServicioUseCaseTest {

    private fun details(title: String) = ServiceDetails(
        title = title,
        photoUrl = "https://example.com/$title.jpg",
        description = "Descripción de $title",
        priceCents = 3500,
        durationMinutes = 60,
        modality = ServiceModality.AMBOS,
    )

    private suspend fun FakeServiceRepository.seed(providerId: String, title: String): Service =
        (AgregarServicioUseCase(this)(providerId, details(title)) as DomainResult.Success<Service>).value

    @Test
    fun `enabling a disabled service sets isActive to true and keeps the rest intact`() = runTest {
        val repository = FakeServiceRepository()
        val added = repository.seed("provider-1", "Manicure")
        DeshabilitarServicioUseCase(repository)(added.id)

        val result = HabilitarServicioUseCase(repository)(added.id)

        assertIs<DomainResult.Success<Service>>(result)
        assertTrue(result.value.isActive)
        assertEquals(added, result.value)
    }

    @Test
    fun `enabling an already active service succeeds and leaves it unchanged`() = runTest {
        val repository = FakeServiceRepository()
        val added = repository.seed("provider-1", "Manicure")

        val result = HabilitarServicioUseCase(repository)(added.id)

        assertIs<DomainResult.Success<Service>>(result)
        assertEquals(added, result.value)
    }

    @Test
    fun `enabling an unknown service propagates NotFound`() = runTest {
        val result = HabilitarServicioUseCase(FakeServiceRepository())("missing")

        assertIs<DomainResult.Failure>(result)
        assertEquals(DomainError.NotFound, result.error)
    }
}
