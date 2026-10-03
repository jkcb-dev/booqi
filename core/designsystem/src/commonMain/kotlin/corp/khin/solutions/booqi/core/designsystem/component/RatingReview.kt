package corp.khin.solutions.booqi.core.designsystem.component

/**
 * One individual review as [RatingDisplay] shows it. Presentation-shaped on purpose (no domain
 * type): [author] is whatever label the feature has for the reviewer, [dateLabel] an already
 * formatted date (or `null`), [comment] the optional text.
 */
data class RatingReview(
    val id: String,
    val stars: Int,
    val author: String,
    val comment: String? = null,
    val dateLabel: String? = null,
)
