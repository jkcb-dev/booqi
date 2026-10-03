package corp.khin.solutions.booqi.domain.model

import kotlin.math.PI
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * A point on the map: [latitude]/[longitude] in decimal degrees. Value object — equality by value,
 * no identity. It is the lat/lng half of [Address] (same field names; see [Address.point]) so a
 * Customer's GPS fix, a saved address pin and a Provider's location are all compared in one shape.
 *
 * Catalog distance filtering (docs/domain/customer-flow.md § Grupo 1) is a simple great-circle
 * ([distanceKmTo], haversine) — no polygons or zones. Not self-validating (same reasoning as
 * [Rating]): [isValid] lets a use case hand back
 * [corp.khin.solutions.booqi.core.common.DomainError.InvalidInput] instead of throwing.
 */
data class GeoPoint(
    val latitude: Double,
    val longitude: Double,
) {
    /** True when both coordinates are inside their legal range (finite, lat ±90, lng ±180). */
    val isValid: Boolean
        get() = latitude in -MAX_LATITUDE..MAX_LATITUDE && longitude in -MAX_LONGITUDE..MAX_LONGITUDE

    /** Great-circle distance to [other] in kilometres (haversine, spherical Earth). */
    fun distanceKmTo(other: GeoPoint): Double {
        val lat1 = latitude.toRadians()
        val lat2 = other.latitude.toRadians()
        val dLat = lat2 - lat1
        val dLng = (other.longitude - longitude).toRadians()
        val a = sin(dLat / 2) * sin(dLat / 2) + cos(lat1) * cos(lat2) * sin(dLng / 2) * sin(dLng / 2)
        return 2 * EARTH_RADIUS_KM * asin(sqrt(a.coerceIn(0.0, 1.0)))
    }

    private fun Double.toRadians(): Double = this * (PI / HALF_TURN_DEGREES)

    private companion object {
        const val EARTH_RADIUS_KM = 6371.0088
        const val MAX_LATITUDE = 90.0
        const val MAX_LONGITUDE = 180.0
        const val HALF_TURN_DEGREES = 180.0
    }
}
