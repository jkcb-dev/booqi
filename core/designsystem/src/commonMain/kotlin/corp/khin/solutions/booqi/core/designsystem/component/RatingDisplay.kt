package corp.khin.solutions.booqi.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import corp.khin.solutions.booqi.core.designsystem.theme.BooqiCornerRadius
import corp.khin.solutions.booqi.core.designsystem.theme.BooqiSpacing
import corp.khin.solutions.booqi.core.designsystem.theme.LocalBooqiExtendedColors
import kotlin.math.roundToInt

private const val MAX_STARS = 5
private const val TENTHS = 10

/**
 * Rating organism (docs/design/DESIGN_SYSTEM.md — rating summary + histogram + individual
 * reviews; Figma P11 and C4). Lives in `core:designsystem` because both `feature:provider` (the
 * Provider previewing their profile) and `feature:browse` (the Customer's view) show it. Takes
 * primitives and [RatingReview] only, so the design system never depends on `domain`.
 *
 * [average] is `null` and [count] is 0 for a Provider without ratings: the summary then reads
 * "Sin calificaciones todavía" instead of a score. The histogram is computed from [reviews]. The
 * review list is a plain [Column] (not lazy) because this is meant to sit inside a parent that
 * already scrolls.
 */
@Composable
fun RatingDisplay(
    average: Double?,
    count: Int,
    reviews: List<RatingReview>,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(BooqiSpacing.md)) {
        RatingSummary(average = average, count = count)
        if (reviews.isNotEmpty()) {
            RatingHistogram(reviews)
            Column(verticalArrangement = Arrangement.spacedBy(BooqiSpacing.sm)) {
                reviews.forEach { ReviewCard(it) }
            }
        }
    }
}

/** The "Rating summary" molecule: numeric score + star row + review count. */
@Composable
private fun RatingSummary(average: Double?, count: Int) {
    if (average == null || count == 0) {
        Text(
            text = "Sin calificaciones todavía",
            style = MaterialTheme.typography.bodyMedium,
            color = LocalBooqiExtendedColors.current.ink2,
        )
        return
    }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(BooqiSpacing.sm)) {
        Text(formatAverage(average), style = MaterialTheme.typography.displaySmall)
        Column {
            StarRow(stars = average.roundToInt())
            Text(
                text = if (count == 1) "1 calificación" else "$count calificaciones",
                style = MaterialTheme.typography.bodySmall,
                color = LocalBooqiExtendedColors.current.ink2,
            )
        }
    }
}

@Composable
private fun RatingHistogram(reviews: List<RatingReview>) {
    Column(verticalArrangement = Arrangement.spacedBy(BooqiSpacing.xxs)) {
        for (stars in MAX_STARS downTo 1) {
            val inBucket = reviews.count { it.stars == stars }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(BooqiSpacing.xs),
            ) {
                Text("$stars", style = MaterialTheme.typography.labelSmall)
                LinearProgressIndicator(
                    progress = { inBucket.toFloat() / reviews.size },
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = "$inBucket",
                    style = MaterialTheme.typography.labelSmall,
                    color = LocalBooqiExtendedColors.current.ink2,
                )
            }
        }
    }
}

@Composable
private fun ReviewCard(review: RatingReview) {
    Card(shape = RoundedCornerShape(BooqiCornerRadius.medium), modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(BooqiSpacing.md),
            verticalArrangement = Arrangement.spacedBy(BooqiSpacing.xxs),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(review.author, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                review.dateLabel?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = LocalBooqiExtendedColors.current.ink2,
                    )
                }
            }
            StarRow(review.stars)
            review.comment?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
        }
    }
}

@Composable
private fun StarRow(stars: Int) {
    val filled = MaterialTheme.colorScheme.secondary
    val empty = LocalBooqiExtendedColors.current.ink3
    Text(
        text = buildAnnotatedString {
            for (index in 1..MAX_STARS) {
                withStyle(SpanStyle(color = if (index <= stars) filled else empty)) { append("★") }
            }
        },
        style = MaterialTheme.typography.bodyLarge,
    )
}

/** `4.5`, `3.0` — one decimal, as the score reads on the summary. */
internal fun formatAverage(average: Double): String {
    val tenths = (average * TENTHS).roundToInt()
    return "${tenths / TENTHS}.${tenths % TENTHS}"
}
