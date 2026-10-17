package corp.khin.solutions.booqi.feature.booking

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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import corp.khin.solutions.booqi.core.designsystem.component.StatusBadge
import corp.khin.solutions.booqi.core.designsystem.theme.BooqiCornerRadius
import corp.khin.solutions.booqi.core.designsystem.theme.BooqiSpacing
import corp.khin.solutions.booqi.core.designsystem.theme.LocalBooqiExtendedColors
import corp.khin.solutions.booqi.domain.model.Booking
import corp.khin.solutions.booqi.domain.model.BookingStatus

/**
 * The opened Booking: the status hero + the facts the Booking snapshotted at request time, then the
 * section of its variant — [PendingSection] (C8), [ConfirmedSection] (C9), [RejectedSection] (C10),
 * [ExpiredSection] (C10b), [ClosedSection] (completed / cancelled). A notice (e.g. "Cita cancelada")
 * and an inline action error sit on top.
 */
@Composable
internal fun BookingDetailBody(state: BookingDetailUiState, onAction: (BookingDetailAction) -> Unit) {
    val booking = state.booking ?: return
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(BooqiSpacing.md)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(BooqiSpacing.md),
    ) {
        state.notice?.let { NoticeBanner(it, onDismiss = { onAction(BookingDetailAction.DismissNotice) }) }
        state.actionError?.let {
            Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
        }
        StatusHero(state = state)
        BookingFactsCard(booking = booking, serviceTitle = state.serviceTitle, providerName = state.providerName)
        when (state.variant) {
            BookingDetailVariant.PENDING -> PendingSection()
            BookingDetailVariant.CONFIRMED -> ConfirmedSection(state = state, onAction = onAction)
            BookingDetailVariant.REJECTED -> RejectedSection(booking = booking, onAction = onAction)
            BookingDetailVariant.EXPIRED -> ExpiredSection(onAction = onAction)
            BookingDetailVariant.COMPLETED, BookingDetailVariant.CANCELLED ->
                ClosedSection(state = state, onAction = onAction)
            null -> Unit
        }
    }
}

/** The glyph, status badge and headline sentence of a variant, with its countdown when it has one. */
@Composable
private fun StatusHero(state: BookingDetailUiState) {
    val booking = state.booking ?: return
    val variant = state.variant ?: return
    val status = if (variant == BookingDetailVariant.EXPIRED) {
        BookingStatus.EXPIRED
    } else {
        booking.status
    }
    Column(verticalArrangement = Arrangement.spacedBy(BooqiSpacing.xs)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(BooqiSpacing.sm),
        ) {
            // There is no icon set yet (see EmptyState); the glyph is decorative, the badge says it.
            Text(
                text = variant.glyph(),
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.clearAndSetSemantics { },
            )
            StatusBadge(text = status.customerLabel(), style = status.badgeStyle())
        }
        Text(variant.headline(), style = MaterialTheme.typography.titleMedium)
        state.countdown?.let { countdown ->
            Text(
                text = if (variant == BookingDetailVariant.PENDING) {
                    "Tiempo restante para que el proveedor responda: $countdown"
                } else {
                    "Tu cita es en $countdown"
                },
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}

private fun BookingDetailVariant.glyph(): String = when (this) {
    BookingDetailVariant.PENDING -> "🕒"
    BookingDetailVariant.CONFIRMED, BookingDetailVariant.COMPLETED -> "✓"
    BookingDetailVariant.REJECTED, BookingDetailVariant.EXPIRED, BookingDetailVariant.CANCELLED -> "✕"
}

private fun BookingDetailVariant.headline(): String = when (this) {
    BookingDetailVariant.PENDING -> "Esperando la respuesta del proveedor"
    BookingDetailVariant.CONFIRMED -> "¡Tu cita está confirmada!"
    BookingDetailVariant.REJECTED -> "El proveedor no pudo aceptar tu solicitud"
    BookingDetailVariant.EXPIRED -> "El proveedor no respondió a tiempo"
    BookingDetailVariant.COMPLETED -> "Tu cita ya se realizó"
    BookingDetailVariant.CANCELLED -> "Esta reserva fue cancelada"
}

@Composable
private fun BookingFactsCard(booking: Booking, serviceTitle: String?, providerName: String?) {
    Card(shape = RoundedCornerShape(BooqiCornerRadius.medium), modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(BooqiSpacing.md),
            verticalArrangement = Arrangement.spacedBy(BooqiSpacing.sm),
        ) {
            Text(serviceTitle ?: SERVICE_FALLBACK_LABEL, style = MaterialTheme.typography.titleMedium)
            LabeledValue(label = "Proveedor", value = providerName ?: PROVIDER_FALLBACK_LABEL)
            LabeledValue(label = "Fecha y hora", value = booking.whenText())
            LabeledValue(label = "Duración", value = formatDuration(booking.durationMinutesSnapshot))
            LabeledValue(label = "Precio", value = booking.priceText())
            booking.deliveryAddress?.let { LabeledValue(label = "Dirección", value = it.line) }
            booking.customerNote?.let { LabeledValue(label = "Tu nota", value = it) }
        }
    }
}

@Composable
private fun NoticeBanner(message: String, onDismiss: () -> Unit) {
    Card(shape = RoundedCornerShape(BooqiCornerRadius.medium), modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(start = BooqiSpacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = LocalBooqiExtendedColors.current.ink2,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onDismiss) { Text("Cerrar") }
        }
    }
}
