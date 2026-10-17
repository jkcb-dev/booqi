package corp.khin.solutions.booqi.feature.booking

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import corp.khin.solutions.booqi.core.designsystem.theme.BooqiSpacing
import corp.khin.solutions.booqi.core.designsystem.theme.LocalBooqiExtendedColors
import corp.khin.solutions.booqi.domain.model.ServiceDetail
import corp.khin.solutions.booqi.domain.model.ServiceModality

private const val RESPONSE_WINDOW_NOTICE = "El proveedor tiene 24h para aceptar o rechazar"

/**
 * C7 — "Confirmar reserva": the full summary, the delivery choice (only for "Local & Dom."), the
 * address section when the appointment is at the Customer's home, the optional "Nota para el
 * proveedor", the 24h notice and "Enviar solicitud". Errors and the access notice (sign in /
 * verify email) show inline; the button is disabled while sending and while a Domicilio request
 * has no usable address.
 */
@Composable
internal fun BookingConfirmContent(
    state: BookingFlowUiState,
    detail: ServiceDetail,
    onAction: (BookingFlowAction) -> Unit,
) {
    val slot = state.selectedSlot
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(BooqiSpacing.md)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(BooqiSpacing.md),
    ) {
        if (slot != null) BookingSummaryCard(detail = detail, slot = slot, state = state)
        if (detail.service.modality == ServiceModality.AMBOS) {
            DeliveryChoice(atHome = state.atHome, enabled = !state.isSubmitting, onAction = onAction)
        }
        if (state.wantsDelivery) BookingAddressSection(state = state, onAction = onAction)
        OutlinedTextField(
            value = state.note,
            onValueChange = { onAction(BookingFlowAction.NoteChanged(it)) },
            label = { Text("Nota para el proveedor (opcional)") },
            enabled = !state.isSubmitting,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            text = RESPONSE_WINDOW_NOTICE,
            style = MaterialTheme.typography.bodyMedium,
            color = LocalBooqiExtendedColors.current.ink2,
        )
        state.submitError?.let {
            Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
        }
        state.access?.let { BookingAccessNotice(issue = it, onAction = onAction) }
        Button(
            onClick = { onAction(BookingFlowAction.Submit) },
            enabled = state.canSubmit,
            modifier = Modifier.fillMaxWidth(),
        ) { Text(if (state.isSubmitting) "Enviando..." else "Enviar solicitud") }
    }
}
