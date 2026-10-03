package corp.khin.solutions.booqi.feature.provider

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import corp.khin.solutions.booqi.core.designsystem.theme.BooqiCornerRadius
import corp.khin.solutions.booqi.core.designsystem.theme.BooqiSpacing
import corp.khin.solutions.booqi.core.designsystem.theme.LocalBooqiExtendedColors
import corp.khin.solutions.booqi.domain.model.Booking

/**
 * The in-screen detail of an opened booking: P9 for a pending request (with the visible "Nota del
 * cliente"), P10 for a confirmed appointment. The facts are what the Booking snapshotted at
 * request time; the actions underneath depend on its status, see [BookingDetailActions].
 */
@Composable
internal fun BookingDetailContent(
    state: BookingInboxUiState,
    detail: BookingDetailUiState,
    onAction: (BookingInboxAction) -> Unit,
) {
    val booking = detail.booking
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(BooqiSpacing.md)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(BooqiSpacing.md),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = state.serviceTitles[booking.serviceId] ?: SERVICE_FALLBACK_LABEL,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f),
            )
            BookingStatusChip(booking.status)
        }
        BookingFactsCard(booking)
        CustomerNote(booking.customerNote)
        booking.reason?.let { reason ->
            LabeledValue(label = "Motivo", value = reason.text())
        }
        BookingDetailActions(state = state, detail = detail, onAction = onAction)
    }
}

@Composable
private fun BookingFactsCard(booking: Booking) {
    Card(shape = RoundedCornerShape(BooqiCornerRadius.medium), modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(BooqiSpacing.md),
            verticalArrangement = Arrangement.spacedBy(BooqiSpacing.sm),
        ) {
            LabeledValue(label = "Cliente", value = CLIENT_LABEL)
            LabeledValue(label = "Fecha y hora", value = booking.whenText())
            LabeledValue(label = "Duración", value = "${booking.durationMinutesSnapshot} min")
            LabeledValue(label = "Precio", value = booking.priceText())
            booking.deliveryAddress?.let { LabeledValue(label = "Dirección", value = it.line) }
        }
    }
}

/** P9's "Nota del cliente" is always shown; an empty one says so instead of disappearing. */
@Composable
private fun CustomerNote(note: String?) {
    Column(verticalArrangement = Arrangement.spacedBy(BooqiSpacing.xxs)) {
        Text("Nota del cliente", style = MaterialTheme.typography.titleSmall)
        Text(
            text = note ?: "Sin nota",
            style = MaterialTheme.typography.bodyMedium,
            color = if (note == null) LocalBooqiExtendedColors.current.ink2 else MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun LabeledValue(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(BooqiSpacing.xxs)) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = LocalBooqiExtendedColors.current.ink2)
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}
