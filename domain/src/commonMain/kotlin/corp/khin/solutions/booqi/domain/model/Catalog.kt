package corp.khin.solutions.booqi.domain.model

import kotlin.time.Instant

/**
 * Read models of the Catalog context (docs/domain/customer-flow.md § Grupo 1, screens C1–C4). They
 * are what the Customer's screens consume — flat views assembled from the `Service` and
 * `ProviderProfile` aggregates by the Catalog queries, never stored and never mutated. (Unlike
 * `Booking`, which must reference those aggregates by ID, a read model may carry a copy of what a
 * screen shows.)
 */

/**
 * The public face of a [ProviderProfile]: what a Customer sees next to a Service and on the
 * Provider's page. Deliberately leaves out `userId`, the pause range and anything internal.
 * [ratingAverage]/[ratingCount] are the profile's rating aggregate (`null`/0 = no ratings yet).
 */
data class ProviderSummary(
    val id: String,
    val name: String,
    val photoUrl: String?,
    val description: String?,
    val location: String?,
    val ratingAverage: Double?,
    val ratingCount: Int,
)

/** Maps a profile to its public face; a complete profile always has a name, `""` otherwise. */
fun ProviderProfile.toSummary(): ProviderSummary = ProviderSummary(
    id = id,
    name = name.orEmpty(),
    photoUrl = photoUrl,
    description = description,
    location = location,
    ratingAverage = ratingAverage,
    ratingCount = ratingCount,
)

/** A [Service] joined to the [ProviderProfile] that owns it (`Service.providerId == ProviderProfile.id`). */
data class CatalogEntry(
    val service: Service,
    val provider: ProviderProfile,
)

/**
 * What the Customer typed/selected on C1/C2.
 *
 * - [text]: free text; every whitespace-separated word must appear in the Service's title or
 *   description, ignoring case and Spanish accents. Blank = no text filter.
 * - [category]: a chip; `null` = "Todos".
 * - [location]: the Customer's GPS point. With it, results carry their distance and are ordered
 *   nearest-first. Without it, no distance is computed.
 * - [radiusKm]: the distance filter (C2: 1/2/5/10 km). Needs [location]; excludes Providers farther
 *   than this (the boundary is included) **and** Providers without coordinates.
 */
data class ServiceSearchCriteria(
    val text: String? = null,
    val category: ServiceCategory? = null,
    val location: GeoPoint? = null,
    val radiusKm: Double? = null,
)

/**
 * One C2 result card: the [service], its [provider]'s public face (name, rating average/count) and,
 * when the search had a [ServiceSearchCriteria.location] and the Provider has coordinates, the
 * [distanceKm] from that point.
 */
data class ServiceSearchResult(
    val service: Service,
    val provider: ProviderSummary,
    val distanceKm: Double? = null,
)

/** C3: a [Service] with its [provider]'s public face (name/photo/rating). */
data class ServiceDetail(
    val service: Service,
    val provider: ProviderSummary,
)

/**
 * One review on a Provider's public page: a rated, completed Booking reduced to what is public —
 * [stars], [comment] and when it was completed. Reviewer names need the Identity context (#50);
 * the Booking's note, address and ids are not exposed to the Customer's screens.
 */
data class ProviderReview(
    val bookingId: String,
    val stars: Int,
    val comment: String?,
    val completedAt: Instant?,
)

/**
 * C4: a Provider's public page — [provider] (bio, location, rating aggregate), its **active**
 * [services] in creation order, and its [reviews] newest first.
 */
data class ProviderPublicProfile(
    val provider: ProviderSummary,
    val services: List<Service>,
    val reviews: List<ProviderReview>,
)
