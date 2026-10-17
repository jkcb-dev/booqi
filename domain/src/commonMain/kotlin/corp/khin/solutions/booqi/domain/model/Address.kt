package corp.khin.solutions.booqi.domain.model

/**
 * A physical address with a map position. Value object — equality by value, no identity.
 *
 * Introduced for `Booking.deliveryAddress`: the *snapshot* of where a Domicilio appointment takes
 * place, copied at request time so a later change of the Customer's saved address never rewrites
 * an existing Booking (docs/DOMAIN.md § Aggregate boundaries). It is intentionally minimal — a
 * display [line] plus the map pin ([latitude]/[longitude], Google Maps search or pin selection,
 * docs/domain/customer-flow.md § Grupo 2).
 *
 * The Customer's single *saved* address ([User.address], `profiles.address_*`, ticket #22) reuses
 * this same class rather than defining a second address shape. Both coordinates are required:
 * the address always comes from a Google Maps search result or a map pin, which carry a position.
 */
data class Address(
    val line: String,
    val latitude: Double,
    val longitude: Double,
) {
    /** The map pin of this address as a [GeoPoint] (same lat/lng, no copy of meaning). */
    val point: GeoPoint get() = GeoPoint(latitude, longitude)
}
