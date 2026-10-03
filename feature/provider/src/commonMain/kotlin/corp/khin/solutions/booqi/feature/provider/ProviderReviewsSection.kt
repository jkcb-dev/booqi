package corp.khin.solutions.booqi.feature.provider

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import corp.khin.solutions.booqi.core.common.DomainError
import corp.khin.solutions.booqi.core.designsystem.component.RatingDisplay
import corp.khin.solutions.booqi.core.designsystem.component.RatingReview
import corp.khin.solutions.booqi.core.designsystem.theme.BooqiSpacing
import corp.khin.solutions.booqi.domain.model.Booking
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.koin.compose.viewmodel.koinViewModel

private const val LOAD_ERROR_MESSAGE = "No pudimos cargar tus calificaciones"

/**
 * P11 — "Calificaciones recibidas" on the completed profile: the shared `RatingDisplay`
 * (average + count + histogram + individual reviews) as Customers see it. Loads fresh every time
 * it enters composition. Reviewers' names don't exist yet (no Identity context), so each review
 * reads "Cliente", see [CLIENT_LABEL].
 */
@Composable
internal fun ProviderReviewsSection(viewModel: ProviderReviewsViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) { viewModel.onAction(ProviderReviewsAction.Start) }

    ProviderReviewsContent(state = state, onRetry = { viewModel.onAction(ProviderReviewsAction.Start) })
}

@Composable
internal fun ProviderReviewsContent(state: ProviderReviewsUiState, onRetry: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(BooqiSpacing.sm)) {
        Text("Calificaciones recibidas", style = MaterialTheme.typography.titleMedium)
        when {
            state.isLoading && state.reviews.isEmpty() -> CircularProgressIndicator()
            state.error != null && state.reviews.isEmpty() -> ReviewsLoadError(state.error, onRetry)
            else -> RatingDisplay(
                average = state.average,
                count = state.count,
                reviews = state.reviews.mapNotNull { it.toRatingReview(TimeZone.currentSystemDefault()) },
            )
        }
    }
}

@Composable
private fun ReviewsLoadError(error: DomainError, onRetry: () -> Unit) {
    Text(
        text = error.describe(notFound = LOAD_ERROR_MESSAGE),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.error,
    )
    OutlinedButton(onClick = onRetry) { Text("Reintentar") }
}

/** One review row: the stars and optional comment of a rated Booking, dated by its completion. */
internal fun Booking.toRatingReview(timeZone: TimeZone): RatingReview? {
    val rating = rating ?: return null
    return RatingReview(
        id = id,
        stars = rating.stars,
        author = CLIENT_LABEL,
        comment = rating.comment,
        dateLabel = completedAt?.toLocalDateTime(timeZone)?.date?.spanishText(),
    )
}
