package corp.khin.solutions.booqi.feature.booking

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import corp.khin.solutions.booqi.core.designsystem.component.ReasonSummary
import corp.khin.solutions.booqi.core.designsystem.theme.BooqiSpacing
import corp.khin.solutions.booqi.core.designsystem.theme.LocalBooqiExtendedColors
import corp.khin.solutions.booqi.domain.model.BookingStatus
import corp.khin.solutions.booqi.domain.model.Rating

/**
 * The two closed variants that have no Figma screen of their own: a completed appointment (with
 * "Calificar" while it has no rating, C12, or the stars the Customer gave) and a cancelled one
 * (with the reason, and the same ways on as a rejection).
 */
@Composable
internal fun ClosedSection(state: BookingDetailUiState, onAction: (BookingDetailAction) -> Unit) {
    val booking = state.booking ?: return
    Column(verticalArrangement = Arrangement.spacedBy(BooqiSpacing.md)) {
        booking.reason?.let {
            val title = if (booking.status == BookingStatus.CANCELLED_BY_CUSTOMER) {
                "Motivo de tu cancelación"
            } else {
                "Motivo de la cancelación"
            }
            ReasonSummary(title = title, reason = it.label(), note = it.note)
        }
        booking.rating?.let { rating ->
            Text(
                text = "Tu calificación",
                style = MaterialTheme.typography.labelSmall,
                color = LocalBooqiExtendedColors.current.ink2,
            )
            Text(
                text = "★".repeat(rating.stars) + "☆".repeat(MAX_STARS - rating.stars),
                style = MaterialTheme.typography.titleMedium,
            )
            rating.comment?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
        }
        if (state.canRate) {
            Button(
                onClick = { onAction(BookingDetailAction.RateClicked) },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Calificar") }
        }
        if (booking.status != BookingStatus.COMPLETED) RebookOptions(onAction)
    }
}

private const val MAX_STARS = Rating.MAX_STARS
