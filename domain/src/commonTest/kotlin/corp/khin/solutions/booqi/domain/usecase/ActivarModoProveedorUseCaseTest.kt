package corp.khin.solutions.booqi.domain.usecase

import corp.khin.solutions.booqi.core.common.DomainResult
import corp.khin.solutions.booqi.domain.model.ProviderProfile
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

/**
 * Escenario: "Un usuario activa el modo Proveedor" (docs/domain/provider-flow.md § Grupo 1).
 */
class ActivarModoProveedorUseCaseTest {

    @Test
    fun `activating provider mode creates an empty profile associated to the account`() = runTest {
        val useCase = ActivarModoProveedorUseCase(FakeProviderProfileRepository())

        val result = useCase("user-1")

        assertIs<DomainResult.Success<ProviderProfile>>(result)
        assertEquals("user-1", result.value.userId)
        assertEquals(false, result.value.isComplete)
        assertNull(result.value.name)
        assertNull(result.value.location)
    }

    @Test
    fun `activating provider mode twice for the same account returns the same profile`() = runTest {
        val repository = FakeProviderProfileRepository()
        val useCase = ActivarModoProveedorUseCase(repository)

        val first = useCase("user-1") as DomainResult.Success<ProviderProfile>
        val second = useCase("user-1") as DomainResult.Success<ProviderProfile>

        assertEquals(first.value.id, second.value.id)
    }
}
