package corp.khin.solutions.booqi.domain.usecase

import corp.khin.solutions.booqi.core.common.DomainResult
import corp.khin.solutions.booqi.domain.model.Availability
import corp.khin.solutions.booqi.domain.repository.AvailabilityRepository

/**
 * Escenario: "El Proveedor consulta su horario" (docs/domain/provider-flow.md § Grupo 3). Loads
 * the Provider's whole [Availability] — weekly hours for the schedule editor (Figma P6) and
 * blocked periods for the blocking calendar (P7) — in one call, since both screens read the same
 * aggregate. A Provider who never defined anything gets an empty [Availability], not an error.
 */
class ObtenerHorarioUseCase(
    private val repository: AvailabilityRepository,
) {
    suspend operator fun invoke(providerId: String): DomainResult<Availability> =
        repository.getAvailability(providerId)
}
