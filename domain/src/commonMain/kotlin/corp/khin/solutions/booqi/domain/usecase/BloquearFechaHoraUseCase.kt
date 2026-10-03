package corp.khin.solutions.booqi.domain.usecase

import corp.khin.solutions.booqi.core.common.DomainResult
import corp.khin.solutions.booqi.core.common.asFailure
import corp.khin.solutions.booqi.domain.model.Availability
import corp.khin.solutions.booqi.domain.model.BlockedPeriod
import corp.khin.solutions.booqi.domain.repository.AvailabilityRepository

/**
 * Escenario: "El Proveedor bloquea un día específico" (docs/domain/provider-flow.md § Grupo 3).
 * Backs tapping a date in the blocking calendar (Figma P7).
 *
 * [period] blocks a whole day (`timeRange == null`) or a time range within it. A blocked time
 * range must end after it starts ([corp.khin.solutions.booqi.core.common.DomainError.
 * InvalidInput], before any repository call). Idempotent: blocking an already-blocked period
 * changes nothing. No rule about past dates — the doc doesn't ask for one.
 *
 * Deliberately does not touch `Booking` (it doesn't exist yet — #18/#25): "las citas ya aceptadas
 * ese día no se ven afectadas" holds structurally, because this use case has no dependency on a
 * Booking repository and so cannot cancel one — same reasoning as [PausarPerfilUseCase]. A block
 * only removes slots from *new* bookings, via [GenerarTimeSlotsUseCase].
 */
class BloquearFechaHoraUseCase(
    private val repository: AvailabilityRepository,
) {
    suspend operator fun invoke(providerId: String, period: BlockedPeriod): DomainResult<Availability> {
        period.timeRange?.blockedRangeError()?.let { return it.asFailure() }
        return repository.addBlockedPeriod(providerId, period)
    }
}
