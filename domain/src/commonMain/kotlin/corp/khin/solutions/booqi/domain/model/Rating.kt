package corp.khin.solutions.booqi.domain.model

/**
 * A Customer's rating of a completed appointment: [stars] plus an optional [comment]. Embedded in
 * [Booking] (1:1 with a completed Booking, no lifecycle of its own — docs/DOMAIN.md), not an
 * aggregate. Not self-validating (same reasoning as [TimeRange]): "stars in
 * [MIN_STARS]..[MAX_STARS]" is enforced by [Booking.rate], which hands back a
 * [corp.khin.solutions.booqi.core.common.DomainError.InvalidInput] instead of throwing.
 */
data class Rating(
    val stars: Int,
    val comment: String? = null,
) {
    /** True when [stars] is a legal value. */
    val hasValidStars: Boolean get() = stars in MIN_STARS..MAX_STARS

    companion object {
        const val MIN_STARS = 1
        const val MAX_STARS = 5
    }
}
