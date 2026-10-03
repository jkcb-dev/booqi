package corp.khin.solutions.booqi.domain.usecase

import corp.khin.solutions.booqi.domain.model.DateRange
import corp.khin.solutions.booqi.domain.model.ServiceCategory
import corp.khin.solutions.booqi.domain.model.ServiceDetails
import corp.khin.solutions.booqi.domain.model.ServiceModality
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

/** Escenario: "El Cliente ve el detalle de un Servicio" (docs/domain/customer-flow.md § Grupo 1, C3). */
class VerDetalleServicioUseCaseTest {

    private val services = FakeServiceRepository()
    private val profiles = FakeProviderProfileRepository()
    private val verDetalle = VerDetalleServicioUseCase(services, profiles)

    private suspend fun completeProvider(userId: String = "user-1"): String {
        val id = profiles.activateProviderMode(userId).value().id
        profiles.completeProfile(id, "Jane's Nails", "https://example.com/jane.jpg", "Bio", "Calle 1")
        profiles.updateRating(id, 4.5, 8)
        return id
    }

    private suspend fun addService(providerId: String, category: ServiceCategory? = null) = services.addService(
        providerId,
        ServiceDetails(
            "Manicure gel",
            "https://example.com/m.jpg",
            "Esmaltado en gel",
            2550,
            45,
            ServiceModality.AMBOS,
            category,
        ),
    ).value()

    @Test
    fun `it shows the service data and the provider name photo and rating`() = runTest {
        val service = addService(completeProvider(), ServiceCategory.UNAS)

        val detail = verDetalle(service.id).value()

        assertEquals(service, detail.service)
        assertEquals(ServiceCategory.UNAS, detail.service.category)
        assertEquals("Jane's Nails", detail.provider.name)
        assertEquals("https://example.com/jane.jpg", detail.provider.photoUrl)
        assertEquals(4.5, detail.provider.ratingAverage)
        assertEquals(8, detail.provider.ratingCount)
        assertEquals(service.providerId, detail.provider.id)
    }

    @Test
    fun `an unknown service is NotFound`() = runTest {
        verDetalle("missing").assertNotFound()
    }

    @Test
    fun `a disabled service is NotFound for the customer`() = runTest {
        val service = addService(completeProvider())
        services.disableService(service.id)

        verDetalle(service.id).assertNotFound()
    }

    @Test
    fun `a service whose provider id is not a profile id is NotFound with no fallback by user id`() = runTest {
        completeProvider(userId = "user-1")
        val service = addService(providerId = "user-1")

        verDetalle(service.id).assertNotFound()
    }

    @Test
    fun `a service of an incomplete profile is NotFound`() = runTest {
        val draft = profiles.activateProviderMode("user-2").value().id
        val service = addService(draft)

        verDetalle(service.id).assertNotFound()
    }

    @Test
    fun `a paused provider only disappears from search so the detail stays reachable`() = runTest {
        val provider = completeProvider()
        val service = addService(provider)
        profiles.setPausedRange(provider, DateRange(LocalDate(2026, 10, 1), LocalDate(2026, 10, 30)))

        assertEquals(service, verDetalle(service.id).value().service)
    }
}
