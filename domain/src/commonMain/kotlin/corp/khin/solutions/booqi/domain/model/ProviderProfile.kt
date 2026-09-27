package corp.khin.solutions.booqi.domain.model

/**
 * The identity of *who* offers services (docs/DOMAIN.md — Provider Management bounded context).
 * One optional ProviderProfile per `User`. Plain data class, no serialization annotations — those
 * belong on the DTO in `data`, never here.
 *
 * Lifecycle (docs/domain/provider-flow.md § Grupo 1):
 * - Created empty by [corp.khin.solutions.booqi.domain.usecase.ActivarModoProveedorUseCase] when
 *   a User activates Provider mode — [isComplete] starts `false` and the descriptive fields start
 *   `null`.
 * - Filled in by [corp.khin.solutions.booqi.domain.usecase.CompletarPerfilDeProveedorUseCase],
 *   which flips [isComplete] to `true`.
 * - Paused/reactivated (vacation mode) by
 *   [corp.khin.solutions.booqi.domain.usecase.PausarPerfilUseCase], which sets/clears
 *   [pausedRange].
 *
 * [ratingAverage]/[ratingCount] are modeled as they would be *read* today. The doc says the
 * rating is a computed aggregate over the Provider's completed `Booking`s — but `Booking` doesn't
 * exist as an entity yet (separate ticket), so there is nothing to compute from yet. `null`/`0`
 * represents a brand-new profile; wiring real computation from Bookings is out of scope here.
 *
 * [pausedRange] only carries the "vacation mode" span exercised by Grupo 1's PausarPerfil
 * scenarios. The full recurring weekly-schedule Availability model is Grupo 3 (issue #16) and is
 * deliberately not built here ahead of that ticket.
 */
data class ProviderProfile(
    val id: String,
    val userId: String,
    val name: String? = null,
    val photoUrl: String? = null,
    val description: String? = null,
    val location: String? = null,
    val isComplete: Boolean = false,
    val pausedRange: DateRange? = null,
    val ratingAverage: Double? = null,
    val ratingCount: Int = 0,
) {
    /** True while [pausedRange] is set — the profile is in vacation mode and hidden from search. */
    val isPaused: Boolean get() = pausedRange != null
}
