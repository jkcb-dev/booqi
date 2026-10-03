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
 * [ratingAverage]/[ratingCount] are the Provider's rating summary: the mean stars and the number of
 * rated, completed `Booking`s (docs/DOMAIN.md — a computed aggregate, not an incremental counter).
 * They are persisted as a denormalized read value, recomputed from all the Provider's rated
 * Bookings by [corp.khin.solutions.booqi.domain.usecase.RecalcularCalificacionDelProveedorUseCase]
 * every time a rating is left; `null`/`0` means "no ratings yet". Not rounded here — the UI rounds.
 *
 * [coordinates] is the map position of [location] (`provider_profiles.location_lat/lng`), used only
 * by the Catalog's distance filter; `null` until something sets it (nothing in the Provider UI does
 * yet — see the follow-up on #20; real geocoding belongs to the maps work, #24). A Provider without
 * coordinates is simply left out of a distance-filtered search.
 *
 * [pausedRange] only carries the "vacation mode" span exercised by Grupo 1's PausarPerfil
 * scenarios. The recurring weekly schedule and blocked dates are [Availability] (Grupo 3, #16),
 * a separate value object keyed by this profile's id; slot generation takes [pausedRange] as an
 * explicit input rather than [Availability] duplicating it.
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
    val coordinates: GeoPoint? = null,
) {
    /** True while [pausedRange] is set — the profile is in vacation mode and hidden from search. */
    val isPaused: Boolean get() = pausedRange != null
}
