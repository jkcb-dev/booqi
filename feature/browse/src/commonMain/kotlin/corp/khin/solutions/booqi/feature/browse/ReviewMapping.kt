package corp.khin.solutions.booqi.feature.browse

import corp.khin.solutions.booqi.core.designsystem.component.RatingReview
import corp.khin.solutions.booqi.domain.model.ProviderReview
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/** Reviewers' names need the Identity context (#50), so every review reads "Cliente" for now. */
internal const val REVIEWER_LABEL = "Cliente"

/** One C4 review as `RatingDisplay` shows it, dated by the completion of the rated Booking. */
internal fun ProviderReview.toRatingReview(timeZone: TimeZone): RatingReview = RatingReview(
    id = bookingId,
    stars = stars,
    author = REVIEWER_LABEL,
    comment = comment?.takeIf { it.isNotBlank() },
    dateLabel = completedAt?.toLocalDateTime(timeZone)?.date?.spanishText(),
)
