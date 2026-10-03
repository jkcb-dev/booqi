package corp.khin.solutions.booqi.feature.browse

import corp.khin.solutions.booqi.domain.model.GeoPoint

// TEMPORARY: there is no platform location yet (#24 — GPS/map SDKs), so the search runs from this
// one fixed point instead of the Customer's real position. It sits near the two complete profiles
// of `SampleData.providerProfiles()` (Studio Booqi ~1.3 km away, Casa Brillante ~4.4 km away) so
// every distance chip does something visible: 1 km finds nothing, 2 km only Studio Booqi, 5 km and
// 10 km both. Replace with the Customer's real location (and drop this constant) when #24 lands.
private const val TEMPORARY_CUSTOMER_LATITUDE = -34.6100
private const val TEMPORARY_CUSTOMER_LONGITUDE = -58.3700

internal val TEMPORARY_CUSTOMER_LOCATION = GeoPoint(
    latitude = TEMPORARY_CUSTOMER_LATITUDE,
    longitude = TEMPORARY_CUSTOMER_LONGITUDE,
)

/** The C2 distance filter options, in kilometres (docs/design/SCREENS.md). `null` = no filter. */
internal val DISTANCE_OPTIONS_KM: List<Int> = listOf(1, 2, DISTANCE_5_KM, DISTANCE_10_KM)

private const val DISTANCE_5_KM = 5
private const val DISTANCE_10_KM = 10

/** How many "Recientes" C1 remembers. */
internal const val RECENT_SEARCHES_LIMIT = 5
