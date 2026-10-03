package corp.khin.solutions.booqi.feature.provider

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import corp.khin.solutions.booqi.core.designsystem.theme.BooqiCornerRadius
import corp.khin.solutions.booqi.core.designsystem.theme.BooqiSpacing
import corp.khin.solutions.booqi.core.designsystem.theme.LocalBooqiExtendedColors
import corp.khin.solutions.booqi.domain.model.Booking

/**
 * One row of the P8 inbox / P10 agenda: service, client, date/time and price. A pending request
 * ([showRequestActions]) also carries the Aceptar / Rechazar buttons Figma P8 shows; tapping the
 * row opens its detail. The Customer's name does not exist yet, see [CLIENT_LABEL].
 */
@Composable
internal fun BookingListItem(
    booking: Booking,
    serviceTitle: String,
    showRequestActions: Boolean,
    isSubmitting: Boolean,
    onAction: (BookingInboxAction) -> Unit,
) {
    val ink2 = LocalBooqiExtendedColors.current.ink2
    Card(
        onClick = { onAction(BookingInboxAction.OpenBooking(booking.id)) },
        shape = RoundedCornerShape(BooqiCornerRadius.medium),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(BooqiSpacing.md),
            verticalArrangement = Arrangement.spacedBy(BooqiSpacing.xs),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(serviceTitle, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                Text(booking.priceText(), style = MaterialTheme.typography.titleMedium)
            }
            Text(CLIENT_LABEL, style = MaterialTheme.typography.bodyMedium, color = ink2)
            Text(booking.whenText(), style = MaterialTheme.typography.bodyMedium, color = ink2)
            if (!showRequestActions) BookingStatusChip(booking.status)
            if (showRequestActions) {
                RequestRowActions(booking = booking, isSubmitting = isSubmitting, onAction = onAction)
            }
        }
    }
}

@Composable
private fun RequestRowActions(
    booking: Booking,
    isSubmitting: Boolean,
    onAction: (BookingInboxAction) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(BooqiSpacing.sm), modifier = Modifier.fillMaxWidth()) {
        Button(
            onClick = { onAction(BookingInboxAction.Accept(booking.id)) },
            enabled = !isSubmitting,
            modifier = Modifier.weight(1f),
        ) { Text("Aceptar") }
        OutlinedButton(
            onClick = { onAction(BookingInboxAction.OpenBooking(booking.id, ReasonIntent.REJECT)) },
            enabled = !isSubmitting,
            modifier = Modifier.weight(1f),
        ) { Text("Rechazar") }
    }
}
