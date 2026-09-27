package corp.khin.solutions.booqi.domain.usecase

import corp.khin.solutions.booqi.core.common.DomainError
import corp.khin.solutions.booqi.core.common.DomainResult
import corp.khin.solutions.booqi.domain.model.ProviderProfile
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

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
            location = "Av. Siempre Viva 123",
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
            location = "",
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
            location = "   ",
        )

        assertIs<DomainResult.Failure>(result)
        assertIs<DomainError.InvalidInput>(result.error)
    }

    private suspend fun activateProfile(repository: FakeProviderProfileRepository, userId: String): String {
        val result = ActivarModoProveedorUseCase(repository)(userId) as DomainResult.Success<ProviderProfile>
        return result.value.id
    }
}
