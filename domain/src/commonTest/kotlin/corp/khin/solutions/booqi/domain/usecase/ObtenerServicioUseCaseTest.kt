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
 * Escenarios: "El Proveedor consulta un Servicio para editarlo" / "El Proveedor consulta o habilita
 * un Servicio que no existe" (docs/domain/provider-flow.md § Grupo 2).
 */
class ObtenerServicioUseCaseTest {

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
    fun `gets a service by id with all its current data even when disabled`() = runTest {
        val repository = FakeServiceRepository()
        val added = repository.seed("provider-1", "Manicure")
        val disabled = (DeshabilitarServicioUseCase(repository)(added.id) as DomainResult.Success<Service>).value

        val result = ObtenerServicioUseCase(repository)(added.id)

        assertIs<DomainResult.Success<Service>>(result)
        assertEquals(disabled, result.value)
        assertEquals("Manicure", result.value.title)
        assertEquals(ServiceModality.AMBOS, result.value.modality)
    }

    @Test
    fun `getting an unknown service propagates NotFound`() = runTest {
        val result = ObtenerServicioUseCase(FakeServiceRepository())("missing")

        assertIs<DomainResult.Failure>(result)
        assertEquals(DomainError.NotFound, result.error)
    }
}
