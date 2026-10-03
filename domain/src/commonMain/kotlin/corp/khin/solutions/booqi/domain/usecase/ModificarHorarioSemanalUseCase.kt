package corp.khin.solutions.booqi.domain.usecase

import corp.khin.solutions.booqi.core.common.DomainError
import corp.khin.solutions.booqi.core.common.DomainResult
import corp.khin.solutions.booqi.core.common.asFailure
import corp.khin.solutions.booqi.domain.model.Availability
import corp.khin.solutions.booqi.domain.model.DayHours
import corp.khin.solutions.booqi.domain.repository.AvailabilityRepository

/**
 * Escenario: "El Proveedor modifica su horario semanal" (docs/domain/provider-flow.md § Grupo 3).
 *
 * Changes only the days in [changes] of an **existing** schedule — each replaces that day's entry
 * (or adds it if the Provider never worked that day), every other day is kept as is. If the
 * Provider has no schedule yet there is nothing to modify:
 * [corp.khin.solutions.booqi.core.common.DomainError.NotFound] (use
 * [DefinirHorarioSemanalUseCase] for the first definition). Validation is the same as Define's
 * ([weeklyHoursError], applied to [changes]) and runs before any repository call.
 *
 * The change applies from now on: slots are generated from the stored schedule at request time,
 * so only future requests see the new hours. "Las citas ya aceptadas fuera del nuevo horario NO
 * se cancelan automáticamente" holds structurally: this use case has no dependency on `Booking`
 * (it doesn't exist yet — #18/#25), so it cannot cancel one — same reasoning as
 * [PausarPerfilUseCase]. When Booking lands, its slot-exclusion logic must keep honoring an
 * accepted Booking even if its time is no longer inside the weekly hours.
 *
 * The read-merge-save is two repository calls, not atomic; fine for the in-memory fake, to be
 * revisited with the real backend (#27) if concurrent edits from two devices become a concern.
 */
class ModificarHorarioSemanalUseCase(
    private val repository: AvailabilityRepository,
) {
    suspend operator fun invoke(providerId: String, changes: List<DayHours>): DomainResult<Availability> {
        changes.weeklyHoursError()?.let { return it.asFailure() }
        return when (val result = repository.getAvailability(providerId)) {
            is DomainResult.Failure -> result
            is DomainResult.Success -> {
                val current = result.value
                if (current.isScheduleDefined) {
                    val changedDays = changes.map { it.day }.toSet()
                    val merged = current.weeklyHours.filterNot { it.day in changedDays } + changes
                    repository.saveWeeklyHours(providerId, merged)
                } else {
                    DomainError.NotFound.asFailure()
                }
            }
        }
    }
}
