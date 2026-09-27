package corp.khin.solutions.booqi.domain.usecase

import corp.khin.solutions.booqi.core.common.DomainResult
import corp.khin.solutions.booqi.domain.model.DateRange
import corp.khin.solutions.booqi.domain.model.ProviderProfile
import corp.khin.solutions.booqi.domain.repository.ProviderProfileRepository

/**
 * Escenarios: "El Proveedor pausa su perfil por un rango de fechas" / "El Proveedor reactiva su
 * perfil antes de tiempo" (docs/domain/provider-flow.md § Grupo 1).
 *
 * One SRP-scoped use case ("set the profile's paused range") covers both: reactivation is simply
 * pausing with no range ([pausedRange] = `null`), so a second class whose only job would be to
 * call the same repository method with `null` would add ceremony without adding behavior. Setting
 * [pausedRange] to `null` clears it immediately — matching "vuelve a aparecer en las búsquedas
 * inmediatamente" with no date-based logic needed.
 *
 * Deliberately does not touch `Booking` at all — the scenario's "las citas ya aceptadas no se
 * cancelan automáticamente" clause is satisfied structurally: this use case has no dependency on
 * a Booking repository, so it cannot cancel one.
 */
class PausarPerfilUseCase(
    private val repository: ProviderProfileRepository,
) {
    suspend operator fun invoke(profileId: String, pausedRange: DateRange?): DomainResult<ProviderProfile> =
        repository.setPausedRange(profileId, pausedRange)
}
