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
 * The Customer's single *saved* address (`User`/`profiles` address columns, ticket #22) is not
 * built yet; #22 should reuse this class for it rather than defining a second address shape.
 */
data class Address(
    val line: String,
    val latitude: Double,
    val longitude: Double,
)
