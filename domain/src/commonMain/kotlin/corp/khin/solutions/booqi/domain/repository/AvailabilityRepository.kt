package corp.khin.solutions.booqi.domain.repository

import corp.khin.solutions.booqi.core.common.DomainResult
import corp.khin.solutions.booqi.domain.model.Availability
import corp.khin.solutions.booqi.domain.model.BlockedPeriod
import corp.khin.solutions.booqi.domain.model.DayHours

/**
 * Domain-owned contract for a Provider's [Availability] (docs/domain/provider-flow.md § Grupo 3 —
 * Gestión de Horario). The implementation (in `data`) decides how it is sourced/persisted — this
 * interface is the only thing a use case is allowed to know about.
 *
 * Every method returns the Provider's resulting [Availability] (ordered as documented on that
 * class), so a screen can re-render from the response without a second read. Callers must have
 * already validated input (see the Horario use cases) — these methods assume valid input and
 * focus on persistence.
 */
interface AvailabilityRepository {

    /**
     * Escenario: "El Proveedor define su horario semanal" (read side, Figma P6/P7). Returns the
     * Provider's current [Availability]. A [providerId] that never defined anything yields an
     * empty one ([Availability.isScheduleDefined] `false`, no blocked periods), not
     * [corp.khin.solutions.booqi.core.common.DomainError.NotFound] — "no schedule yet" is a valid
     * state (the Provider's first visit to P6).
     */
    suspend fun getAvailability(providerId: String): DomainResult<Availability>

    /**
     * Escenarios: "El Proveedor define su horario semanal" / "...modifica su horario semanal".
     * **Replaces** the whole weekly schedule of [providerId] with [weeklyHours]. Blocked periods
     * are untouched.
     */
    suspend fun saveWeeklyHours(providerId: String, weeklyHours: List<DayHours>): DomainResult<Availability>

    /**
     * Escenario: "El Proveedor bloquea un día específico". Adds [period] to the Provider's blocked
     * periods. Idempotent: blocking an already-blocked period succeeds and changes nothing.
     */
    suspend fun addBlockedPeriod(providerId: String, period: BlockedPeriod): DomainResult<Availability>

    /**
     * Escenario: "El Proveedor desbloquea una fecha". Removes exactly [period] (same date and same
     * time range, or both whole-day). Idempotent: removing a period that isn't blocked succeeds
     * and changes nothing.
     */
    suspend fun removeBlockedPeriod(providerId: String, period: BlockedPeriod): DomainResult<Availability>
}
