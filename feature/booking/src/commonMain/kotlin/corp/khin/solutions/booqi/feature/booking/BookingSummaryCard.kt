package corp.khin.solutions.booqi.feature.booking

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import corp.khin.solutions.booqi.core.designsystem.component.ModalityBadge
import corp.khin.solutions.booqi.core.designsystem.theme.BooqiCornerRadius
import corp.khin.solutions.booqi.core.designsystem.theme.BooqiSpacing
import corp.khin.solutions.booqi.domain.model.ServiceDetail
import corp.khin.solutions.booqi.domain.model.TimeSlot

/** C7's summary: service, provider, date/time, duration, price, modality and — for an appointment
 * at the Customer's home — the address that will be snapshotted. */
@Composable
internal fun BookingSummaryCard(detail: ServiceDetail, slot: TimeSlot, state: BookingFlowUiState) {
    val service = detail.service
    Card(shape = RoundedCornerShape(BooqiCornerRadius.medium), modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(BooqiSpacing.md),
            verticalArrangement = Arrangement.spacedBy(BooqiSpacing.sm),
        ) {
            Text(service.title, style = MaterialTheme.typography.titleMedium)
            LabeledValue(label = "Proveedor", value = detail.provider.name.ifBlank { PROVIDER_FALLBACK_LABEL })
            LabeledValue(
                label = "Fecha y hora",
                value = "${slot.date.spanishText()} · ${formatTime(slot.start)}",
            )
            LabeledValue(label = "Duración", value = formatDuration(service.durationMinutes))
            LabeledValue(label = "Precio", value = formatPrice(service.priceCents))
            ModalityBadge(text = service.modality.label())
            val address = state.savedAddress
            if (state.wantsDelivery && address != null) {
                LabeledValue(label = "Dirección", value = address.line)
            }
        }
    }
}

/** For a "Local & Dom." Service: where the appointment takes place. */
@Composable
internal fun DeliveryChoice(atHome: Boolean, enabled: Boolean, onAction: (BookingFlowAction) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(BooqiSpacing.xxs)) {
        Text("¿Dónde quieres la cita?", style = MaterialTheme.typography.titleSmall)
        DeliveryOption("En el local del proveedor", selected = !atHome, enabled = enabled) {
            onAction(BookingFlowAction.AtHomeChanged(false))
        }
        DeliveryOption("A domicilio", selected = atHome, enabled = enabled) {
            onAction(BookingFlowAction.AtHomeChanged(true))
        }
    }
}

@Composable
private fun DeliveryOption(label: String, selected: Boolean, enabled: Boolean, onSelect: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, enabled = enabled, role = Role.RadioButton, onClick = onSelect),
    ) {
        RadioButton(selected = selected, onClick = null, enabled = enabled)
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(start = BooqiSpacing.sm))
    }
}
