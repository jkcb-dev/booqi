package corp.khin.solutions.booqi.domain.usecase

import corp.khin.solutions.booqi.core.common.DomainError
import corp.khin.solutions.booqi.core.common.DomainResult
import corp.khin.solutions.booqi.domain.model.GeoPoint
import corp.khin.solutions.booqi.domain.model.ProviderLocation
import corp.khin.solutions.booqi.domain.model.ProviderProfile
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

/**
 * Escenarios: "El Proveedor completa su perfil" / "El Proveedor intenta completar el perfil sin
 * ubicación" (docs/domain/provider-flow.md § Grupo 1).
 */
class CompletarPerfilDeProveedorUseCaseTest {

    @Test
    fun `completing profile with all required fields marks it complete`() = runTest {
        val repository = FakeProviderProfileRepository()
        val profileId = activateProfile(repository, "user-1")
        val useCase = CompletarPerfilDeProveedorUseCase(repository)

        val result = useCase(
            profileId = profileId,
            name = "Jane's Nails",
            photoUrl = "https://example.com/jane.jpg",
            description = "Gel & acrylic specialist",
            location = ProviderLocation("Av. Siempre Viva 123"),
        )

        assertIs<DomainResult.Success<ProviderProfile>>(result)
        assertEquals(true, result.value.isComplete)
        assertEquals("Jane's Nails", result.value.name)
        assertEquals("Av. Siempre Viva 123", result.value.location)
    }

    @Test
    fun `completing profile without ubicacion is rejected with a validation error`() = runTest {
        val repository = FakeProviderProfileRepository()
        val profileId = activateProfile(repository, "user-1")
        val useCase = CompletarPerfilDeProveedorUseCase(repository)

        val result = useCase(
            profileId = profileId,
            name = "Jane's Nails",
            photoUrl = "https://example.com/jane.jpg",
            description = "Gel & acrylic specialist",
            location = ProviderLocation(""),
        )

        assertIs<DomainResult.Failure>(result)
        assertIs<DomainError.InvalidInput>(result.error)
    }

    @Test
    fun `completing profile with blank whitespace ubicacion is also rejected`() = runTest {
        val repository = FakeProviderProfileRepository()
        val profileId = activateProfile(repository, "user-1")
        val useCase = CompletarPerfilDeProveedorUseCase(repository)

        val result = useCase(
            profileId = profileId,
            name = "Jane's Nails",
            photoUrl = "https://example.com/jane.jpg",
            description = "Gel & acrylic specialist",
            location = ProviderLocation("   "),
        )

        assertIs<DomainResult.Failure>(result)
        assertIs<DomainError.InvalidInput>(result.error)
    }

    private suspend fun complete(
        useCase: CompletarPerfilDeProveedorUseCase,
        profileId: String,
        location: ProviderLocation,
    ) = useCase(profileId, "Jane's Nails", "https://example.com/jane.jpg", "Gel", location)

    
    fun `a valid pin is stored with the location`() = runTest {
        val repository = FakeProviderProfileRepository()
        val profileId = activateProfile(repository, "user-1")
        val pin = GeoPoint(-34.6037, -58.3816)

        val result = complete(CompletarPerfilDeProveedorUseCase(repository), profileId, ProviderLocation("Centro", pin))

        assertIs<DomainResult.Success<ProviderProfile>>(result)
        assertEquals(pin, result.value.coordinates)
        assertEquals(pin, (repository.getProfile(profileId) as DomainResult.Success).value.coordinates)
    }

    
    fun `invalid coordinates are rejected and nothing is saved`() = runTest {
        val repository = FakeProviderProfileRepository()
        val profileId = activateProfile(repository, "user-1")
        val useCase = CompletarPerfilDeProveedorUseCase(repository)

        listOf(GeoPoint(91.0, 0.0), GeoPoint(0.0, -181.0), GeoPoint(Double.NaN, 0.0)).forEach { bad ->
            val result = complete(useCase, profileId, ProviderLocation("Centro", bad))

            assertIs<DomainResult.Failure>(result)
            assertIs<DomainError.InvalidInput>(result.error)
        }
        val stored = (repository.getProfile(profileId) as DomainResult.Success).value
        assertEquals(false, stored.isComplete)
        assertNull(stored.coordinates)
    }

    
    fun `null coordinates clear a previous pin`() = runTest {
        val repository = FakeProviderProfileRepository()
        val profileId = activateProfile(repository, "user-1")
        val useCase = CompletarPerfilDeProveedorUseCase(repository)
        complete(useCase, profileId, ProviderLocation("Centro", GeoPoint(-34.6, -58.4)))

        val result = complete(useCase, profileId, ProviderLocation("Otra calle"))

        assertIs<DomainResult.Success<ProviderProfile>>(result)
        assertEquals("Otra calle", result.value.location)
        assertNull(result.value.coordinates)
    }

    private suspend fun activateProfile(repository: FakeProviderProfileRepository, userId: String): String {
        val result = ActivarModoProveedorUseCase(repository)(userId) as DomainResult.Success<ProviderProfile>
        return result.value.id
    }
}
