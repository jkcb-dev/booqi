package corp.khin.solutions.booqi.feature.browse

import corp.khin.solutions.booqi.core.common.DomainResult
import corp.khin.solutions.booqi.core.designsystem.component.RatingReview
import corp.khin.solutions.booqi.domain.model.ProviderReview
import corp.khin.solutions.booqi.domain.usecase.ObtenerUsuarioPublicoUseCase
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/** Fallback reviewer label when a name could not be resolved (a name is a nicety, never an error). */
internal const val REVIEWER_LABEL = "Cliente"

/**
 * The names to show for the reviewers [ids] (`ProviderReview.customerId`), by id. A deleted account
 * reads "Usuario eliminado" (decided by the domain, `PublicUser`); an id whose lookup fails is left
 * out and the screen falls back to [REVIEWER_LABEL].
 */
internal suspend fun ObtenerUsuarioPublicoUseCase.namesOf(ids: Set<String>): Map<String, String> =
    ids.mapNotNull { id -> (this(id) as? DomainResult.Success)?.let { id to it.value.displayName } }.toMap()

/** One C4 review as `RatingDisplay` shows it, dated by the completion of the rated Booking. */
internal fun ProviderReview.toRatingReview(timeZone: TimeZone, authorName: String = REVIEWER_LABEL): RatingReview =
    RatingReview(
        id = bookingId,
        stars = stars,
        author = authorName,
        comment = comment?.takeIf { it.isNotBlank() },
        dateLabel = completedAt?.toLocalDateTime(timeZone)?.date?.spanishText(),
    )
