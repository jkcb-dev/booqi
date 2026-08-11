package corp.khin.solutions.booqi.domain.model

/**
 * A bookable local service provider (nail tech, barber, technician, ...). Plain data class, no
 * serialization annotations — those belong on the DTO in `data`, never here.
 */
data class ServiceProvider(
    val id: String,
    val name: String,
    val category: ServiceCategory,
    val ratingOutOf5: Double,
    val priceFromCents: Int,
    val shortTagline: String,
)

enum class ServiceCategory {
    NAILS,
    BARBER,
    TECHNICIAN,
    CLEANING,
    OTHER,
}
