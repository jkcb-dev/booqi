package corp.khin.solutions.booqi.domain.usecase

import corp.khin.solutions.booqi.domain.model.BookingStatus
import corp.khin.solutions.booqi.domain.model.DateRange
import corp.khin.solutions.booqi.domain.model.ProviderReview
import corp.khin.solutions.booqi.domain.model.Rating
import corp.khin.solutions.booqi.domain.model.ServiceDetails
import corp.khin.solutions.booqi.domain.model.ServiceModality
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

/** Escenario: "El Cliente ve el perfil completo de un Proveedor" (docs/domain/customer-flow.md § Grupo 1, C4). */
class VerPerfilProveedorUseCaseTest {

    private val profiles = FakeProviderProfileRepository()
    private val services = FakeServiceRepository()
    private val bookings = FakeBookingRepository()
    private val verPerfil =
        VerPerfilProveedorUseCase(profiles, services, ObtenerCalificacionesDelProveedorUseCase(bookings))

    private suspend fun completeProvider(userId: String = "user-1"): String {
        val id = profiles.activateProviderMode(userId).value().id
        profiles.completeProfile(id, "Jane's Nails", "https://example.com/jane.jpg", "Bio de Jane", "Calle 1")
        return id
    }

    private suspend fun addService(providerId: String, title: String) = services.addService(
        providerId,
        ServiceDetails(title, "https://example.com/$title.jpg", title, 2500, 30, ServiceModality.LOCAL),
    ).value()

    private fun rated(id: String, providerId: String, stars: Int, completedAt: String) = bookings.seed(
        booking(
            id = id,
            providerId = providerId,
            status = BookingStatus.COMPLETED,
            rating = Rating(stars, "Comentario $id"),
            completedAt = Instant.parse(completedAt),
        ),
    )

    @Test
    fun `it shows the bio the location and all the active services of the provider`() = runTest {
        val provider = completeProvider()
        val first = addService(provider, "Manicure")
        val second = addService(provider, "Pedicure")
        addService(completeProvider("user-2"), "Corte")

        val perfil = verPerfil(provider).value()

        assertEquals("Jane's Nails", perfil.provider.name)
        assertEquals("Bio de Jane", perfil.provider.description)
        assertEquals("Calle 1", perfil.provider.location)
        assertEquals(listOf(first, second), perfil.services)
    }

    @Test
    fun `disabled services are left out`() = runTest {
        val provider = completeProvider()
        val kept = addService(provider, "Manicure")
        val off = addService(provider, "Pedicure")
        services.disableService(off.id)

        assertEquals(listOf(kept), verPerfil(provider).value().services)
    }

    @Test
    fun `it shows the overall rating and the individual reviews newest first`() = runTest {
        val provider = completeProvider()
        profiles.updateRating(provider, 4.0, 2)
        rated("old", provider, 5, "2026-10-12T11:00:00Z")
        rated("new", provider, 3, "2026-10-26T11:00:00Z")
        rated("elsewhere", "provider-2", 1, "2026-10-30T11:00:00Z")

        val perfil = verPerfil(provider).value()

        assertEquals(4.0, perfil.provider.ratingAverage)
        assertEquals(2, perfil.provider.ratingCount)
        assertEquals(
            listOf(
                ProviderReview("new", 3, "Comentario new", Instant.parse("2026-10-26T11:00:00Z")),
                ProviderReview("old", 5, "Comentario old", Instant.parse("2026-10-12T11:00:00Z")),
            ),
            perfil.reviews,
        )
    }

    @Test
    fun `a provider with no services and no ratings gets empty lists and no average`() = runTest {
        val perfil = verPerfil(completeProvider()).value()

        assertEquals(emptyList(), perfil.services)
        assertEquals(emptyList(), perfil.reviews)
        assertEquals(null, perfil.provider.ratingAverage)
        assertEquals(0, perfil.provider.ratingCount)
    }

    @Test
    fun `an unknown profile is NotFound and a service owner id that is a user id is not a profile`() = runTest {
        completeProvider(userId = "user-1")

        verPerfil("missing").assertNotFound()
        verPerfil("user-1").assertNotFound()
    }

    @Test
    fun `an incomplete profile is NotFound`() = runTest {
        verPerfil(profiles.activateProviderMode("user-2").value().id).assertNotFound()
    }

    @Test
    fun `the page of a paused provider stays reachable`() = runTest {
        val provider = completeProvider()
        profiles.setPausedRange(provider, DateRange(LocalDate(2026, 10, 1), LocalDate(2026, 10, 30)))

        assertEquals("Jane's Nails", verPerfil(provider).value().provider.name)
    }
}
