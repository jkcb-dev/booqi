package corp.khin.solutions.booqi.domain.usecase

import corp.khin.solutions.booqi.core.common.DomainResult
import corp.khin.solutions.booqi.core.common.asFailure
import corp.khin.solutions.booqi.domain.model.Availability
import corp.khin.solutions.booqi.domain.model.DayHours
import corp.khin.solutions.booqi.domain.repository.AvailabilityRepository

/**
 * Escenario: "El Proveedor define su horario semanal" (docs/domain/provider-flow.md § Grupo 3).
 * Backs the "save" of the weekly editor (Figma P6).
 *
 * **Define vs Modify**: this use case *sets the whole week at once* — [weeklyHours] replaces
 * whatever schedule the Provider had (none, on first use), so it is also safe to call again with a
 * full week. [ModificarHorarioSemanalUseCase] is the partial, existing-schedule-only variant
 * ("cambia sus horas de un día"); they differ in behavior (replace vs. merge, and Modify fails
 * with NotFound when nothing is defined), so they are two classes rather than one with a flag.
 *
 * Validation (shared with Modify, see [weeklyHoursError]): an active day's end must be after its
 * start and a day may appear only once — [corp.khin.solutions.booqi.core.common.DomainError.
 * InvalidInput], returned before the repository is called. Deliberately no "all 7 days required"
 * rule: a Provider may work only some days, and the doc doesn't ask for more. Blocked periods are
 * left untouched.
 *
 * Deliberately does not touch `Booking` at all (it doesn't exist yet — #18/#25): "las citas ya
 * aceptadas no se cancelan" holds structurally, because this use case has no dependency on a
 * Booking repository and so cannot cancel one — same reasoning as [PausarPerfilUseCase].
 */
class DefinirHorarioSemanalUseCase(
    private val repository: AvailabilityRepository,
) {
    suspend operator fun invoke(providerId: String, weeklyHours: List<DayHours>): DomainResult<Availability> {
        weeklyHours.weeklyHoursError()?.let { return it.asFailure() }
        return repository.saveWeeklyHours(providerId, weeklyHours)
    }
}
