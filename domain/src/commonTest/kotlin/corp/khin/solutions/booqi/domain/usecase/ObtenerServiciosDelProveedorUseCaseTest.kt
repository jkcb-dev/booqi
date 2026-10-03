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
 * Escenario: "El Proveedor ve todos sus Servicios, incluidos los deshabilitados"
 * (docs/domain/provider-flow.md § Grupo 2).
 */
class ObtenerServiciosDelProveedorUseCaseTest {

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
    fun `lists every service of the provider including disabled ones in creation order`() = runTest {
        val repository = FakeServiceRepository()
        val first = repository.seed("provider-1", "Manicure")
        val second = repository.seed("provider-1", "Pedicure")
        repository.seed("provider-2", "Corte")
        val disabled = (DeshabilitarServicioUseCase(repository)(first.id) as DomainResult.Success<Service>).value

        val result = ObtenerServiciosDelProveedorUseCase(repository)("provider-1")

        assertIs<DomainResult.Success<List<Service>>>(result)
        assertEquals(listOf(disabled, second), result.value)
        assertFalse(result.value[0].isActive)
        assertTrue(result.value[1].isActive)
    }

    @Test
    fun `a provider without services gets an empty list not an error`() = runTest {
        val result = ObtenerServiciosDelProveedorUseCase(FakeServiceRepository())("provider-1")

        assertIs<DomainResult.Success<List<Service>>>(result)
        assertTrue(result.value.isEmpty())
    }
}
