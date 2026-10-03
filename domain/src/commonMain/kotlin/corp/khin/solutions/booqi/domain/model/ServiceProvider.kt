package corp.khin.solutions.booqi.domain.model

/**
 * A bookable local service provider (nail tech, barber, technician, ...). Plain data class, no
 * serialization annotations — those belong on the DTO in `data`, never here.
 */
@Deprecated(
    "Pre-correction model that conflates Provider and Service (docs/DOMAIN.md). Use ProviderProfile + " +
        "Service via BuscarServiciosUseCase / VerPerfilProveedorUseCase. Kept only for feature:browse; " +
        "remove once BrowseScreen is migrated (#21).",
)
data class ServiceProvider(
    val id: String,
    val name: String,
    val category: LegacyServiceCategory,
    val ratingOutOf5: Double,
    val priceFromCents: Int,
    val shortTagline: String,
)

/** Category of the deprecated [ServiceProvider]; renamed so [ServiceCategory] can be the real one. */
@Deprecated("Belongs to the deprecated ServiceProvider model; use ServiceCategory (on Service). Remove with #21.")
enum class LegacyServiceCategory {
    NAILS,
    BARBER,
    TECHNICIAN,
    CLEANING,
    OTHER,
}
