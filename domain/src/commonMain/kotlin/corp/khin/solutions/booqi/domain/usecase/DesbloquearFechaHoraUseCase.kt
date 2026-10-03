package corp.khin.solutions.booqi.domain.usecase

import corp.khin.solutions.booqi.core.common.DomainResult
import corp.khin.solutions.booqi.domain.model.Availability
import corp.khin.solutions.booqi.domain.model.BlockedPeriod
import corp.khin.solutions.booqi.domain.repository.AvailabilityRepository

/**
 * Escenario: "El Proveedor desbloquea una fecha" (docs/domain/provider-flow.md § Grupo 3).
 * Inverse of [BloquearFechaHoraUseCase]: the blocking calendar (Figma P7) shows blocked dates in a
 * different tint and tapping one must be able to free it again, so without this the Provider could
 * never undo a mistaken block. Not in the original ticket's list; added for that reason.
 *
 * Removes exactly [period] (same date and same time range, or both whole-day). Idempotent: a
 * period that isn't blocked is a successful no-op, so a double tap is harmless. No `Booking`
 * dependency, same as Bloquear.
 */
class DesbloquearFechaHoraUseCase(
    private val repository: AvailabilityRepository,
) {
    suspend operator fun invoke(providerId: String, period: BlockedPeriod): DomainResult<Availability> =
        repository.removeBlockedPeriod(providerId, period)
}
