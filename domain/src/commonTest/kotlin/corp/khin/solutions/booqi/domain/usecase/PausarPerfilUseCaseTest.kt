package corp.khin.solutions.booqi.domain.usecase

import corp.khin.solutions.booqi.core.common.DomainResult
import corp.khin.solutions.booqi.domain.model.DateRange
import corp.khin.solutions.booqi.domain.model.ProviderProfile
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

/**
 * Escenarios: "El Proveedor pausa su perfil por un rango de fechas" / "El Proveedor reactiva su
 * perfil antes de tiempo" (docs/domain/provider-flow.md § Grupo 1).
 */
class PausarPerfilUseCaseTest {

    @Test
    fun `pausing the profile for a date range sets pausedRange and marks it paused`() = runTest {
        val repository = FakeProviderProfileRepository()
        val profileId = activateProfile(repository, "user-1")
        val useCase = PausarPerfilUseCase(repository)
        val range = DateRange(LocalDate(2026, 8, 10), LocalDate(2026, 8, 20))

        val result = useCase(profileId, range)

        assertIs<DomainResult.Success<ProviderProfile>>(result)
        assertEquals(range, result.value.pausedRange)
        assertEquals(true, result.value.isPaused)
        // "las citas ya aceptadas antes de la pausa no se cancelan automáticamente": this use
        // case has no dependency on a Booking repository at all, so it structurally cannot
        // cancel one — there is nothing else to assert here.
    }

    @Test
    fun `reactivating a paused profile clears pausedRange immediately`() = runTest {
        val repository = FakeProviderProfileRepository()
        val profileId = activateProfile(repository, "user-1")
        val useCase = PausarPerfilUseCase(repository)
        useCase(profileId, DateRange(LocalDate(2026, 8, 10), LocalDate(2026, 8, 20)))

        val result = useCase(profileId, null)

        assertIs<DomainResult.Success<ProviderProfile>>(result)
        assertNull(result.value.pausedRange)
        assertEquals(false, result.value.isPaused)
    }

    private suspend fun activateProfile(repository: FakeProviderProfileRepository, userId: String): String {
        val result = ActivarModoProveedorUseCase(repository)(userId) as DomainResult.Success<ProviderProfile>
        return result.value.id
    }
}
