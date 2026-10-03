package corp.khin.solutions.booqi.data.dto

/**
 * Wire/storage shape for `ProviderProfile`. Deliberately not `@Serializable` yet: no real backend
 * contract exists to shape it against (Supabase wiring lands with #27, see docs/DATABASE.md).
 *
 * [pausedRangeStart]/[pausedRangeEnd] are plain ISO-8601 date strings (`"2026-08-10"`) rather than
 * `kotlinx.datetime.LocalDate`, matching how a Postgres `date` column round-trips through JSON —
 * this keeps the DTO from presupposing the domain layer's value types
 * ([corp.khin.solutions.booqi.domain.model.DateRange]). Both `null` means "not paused"; the
 * mapper treats any other combination as a data inconsistency it doesn't try to guess around.
 *
 * [locationLat]/[locationLng] are the `provider_profiles.location_lat/lng` columns; the mapper only
 * builds a domain `GeoPoint` when both are present.
 */
data class ProviderProfileDto(
    val id: String,
    val userId: String,
    val name: String?,
    val photoUrl: String?,
    val description: String?,
    val location: String?,
    val isComplete: Boolean,
    val pausedRangeStart: String?,
    val pausedRangeEnd: String?,
    val ratingAverage: Double?,
    val ratingCount: Int,
    val locationLat: Double?,
    val locationLng: Double?,
)
