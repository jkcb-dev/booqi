package corp.khin.solutions.booqi.data.dto

/**
 * Wire/storage shape for `Service`. Deliberately not `@Serializable` yet — same reasoning as
 * [ProviderProfileDto]: no real backend contract exists to shape it against (Supabase wiring
 * lands with #27, see docs/DATABASE.md).
 *
 * [modality] is a plain `String` matching the `services` table's exact values
 * (`"local" | "domicilio" | "ambos"`, see docs/DATABASE.md) rather than the domain's
 * [corp.khin.solutions.booqi.domain.model.ServiceModality] enum directly — the mapper does that
 * translation, keeping the DTO from presupposing the domain layer's types.
 */
data class ServiceDto(
    val id: String,
    val providerId: String,
    val title: String,
    val photoUrl: String,
    val description: String,
    val priceCents: Int,
    val durationMinutes: Int,
    val modality: String,
    val isActive: Boolean,
)

/**
 * The creatable/editable fields of a [ServiceDto] — mirrors the domain's
 * [corp.khin.solutions.booqi.domain.model.ServiceDetails]. Used by `create()` so the datasource
 * signature stays small; id, owner and `isActive` are assigned by the datasource/caller.
 */
data class ServiceDetailsDto(
    val title: String,
    val photoUrl: String,
    val description: String,
    val priceCents: Int,
    val durationMinutes: Int,
    val modality: String,
)
