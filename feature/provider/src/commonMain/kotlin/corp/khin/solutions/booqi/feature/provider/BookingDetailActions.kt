package corp.khin.solutions.booqi.feature.provider

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import corp.khin.solutions.booqi.core.designsystem.component.ReasonPicker
import corp.khin.solutions.booqi.core.designsystem.component.ReasonPickerCallbacks
import corp.khin.solutions.booqi.core.designsystem.component.ReasonPickerLabels
import corp.khin.solutions.booqi.core.designsystem.component.ReasonPickerState
import corp.khin.solutions.booqi.core.designsystem.theme.BooqiSpacing
import corp.khin.solutions.booqi.core.designsystem.theme.LocalBooqiExtendedColors
import corp.khin.solutions.booqi.domain.model.BookingStatus

/** What P10's complete button reads until the appointment is over (Figma, verbatim). */
private const val COMPLETE_DISABLED_LABEL = "Disponible al finalizar la cita"
private const val COMPLETE_ENABLED_LABEL = "Marcar como completada"
private const val LAPSED_MESSAGE = "Esta solicitud venció: pasaron más de 24 horas sin respuesta."

/**
 * The actions under an opened booking. While the reject/cancel picker is open it replaces them.
 * Otherwise: a pending request offers Aceptar / Rechazar (P9) unless its 24h window already
 * lapsed (a stale row — the domain would refuse it, so the buttons are not offered); a confirmed
 * one offers the complete button — disabled and labelled "Disponible al finalizar la cita" until
 * the appointment's end time has passed — and "Cancelar cita" (P10). A booking in any other
 * status (it may have changed since the row was loaded) has no actions. A failed action shows
 * its message inline, never a crash.
 */
@Composable
internal fun BookingDetailActions(
    state: BookingInboxUiState,
    detail: BookingDetailUiState,
    onAction: (BookingInboxAction) -> Unit,
) {
    val booking = detail.booking
    val isSubmitting = state.submittingBookingId == booking.id
    Column(verticalArrangement = Arrangement.spacedBy(BooqiSpacing.sm)) {
        detail.actionError?.let {
            Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
        }
        val form = detail.reasonForm
        when {
            form != null -> ReasonSection(form = form, isSubmitting = isSubmitting, onAction = onAction)
            booking.status == BookingStatus.REQUESTED && booking.isResponseOverdue(state.now) ->
                Text(
                    text = LAPSED_MESSAGE,
                    style = MaterialTheme.typography.bodyMedium,
                    color = LocalBooqiExtendedColors.current.ink2,
                )
            booking.status == BookingStatus.REQUESTED -> RequestActions(booking.id, isSubmitting, onAction)
            booking.status == BookingStatus.CONFIRMED -> ConfirmedActions(
                canComplete = booking.hasEnded(state.now, state.timeZone),
                isSubmitting = isSubmitting,
                onAction = onAction,
            )
        }
    }
}

@Composable
private fun RequestActions(bookingId: String, isSubmitting: Boolean, onAction: (BookingInboxAction) -> Unit) {
    Button(
        onClick = { onAction(BookingInboxAction.Accept(bookingId)) },
        enabled = !isSubmitting,
        modifier = Modifier.fillMaxWidth(),
    ) { Text("Aceptar") }
    OutlinedButton(
        onClick = { onAction(BookingInboxAction.OpenReasonForm(ReasonIntent.REJECT)) },
        enabled = !isSubmitting,
        modifier = Modifier.fillMaxWidth(),
    ) { Text("Rechazar") }
}

@Composable
private fun ConfirmedActions(canComplete: Boolean, isSubmitting: Boolean, onAction: (BookingInboxAction) -> Unit) {
    Button(
        onClick = { onAction(BookingInboxAction.Complete) },
        enabled = canComplete && !isSubmitting,
        modifier = Modifier.fillMaxWidth(),
    ) { Text(if (canComplete) COMPLETE_ENABLED_LABEL else COMPLETE_DISABLED_LABEL) }
    OutlinedButton(
        onClick = { onAction(BookingInboxAction.OpenReasonForm(ReasonIntent.CANCEL)) },
        enabled = !isSubmitting,
        modifier = Modifier.fillMaxWidth(),
    ) { Text("Cancelar cita") }
}

/** The shared `ReasonPicker` for either flow, plus a way back to the detail without acting. */
@Composable
private fun ReasonSection(form: ReasonFormState, isSubmitting: Boolean, onAction: (BookingInboxAction) -> Unit) {
    val isReject = form.intent == ReasonIntent.REJECT
    ReasonPicker(
        state = ReasonPickerState(
            options = providerReasonOptions,
            selectedId = form.code?.name,
            note = form.note,
            error = form.error,
            isSubmitting = isSubmitting,
        ),
        labels = ReasonPickerLabels(
            title = if (isReject) "Motivo del rechazo" else "Motivo de la cancelación",
            noteLabel = "Detalle (opcional)",
            confirmLabel = if (isReject) "Rechazar solicitud" else "Cancelar cita",
        ),
        callbacks = ReasonPickerCallbacks(
            onSelect = { id -> providerReasonFor(id)?.let { onAction(BookingInboxAction.ReasonSelected(it)) } },
            onNoteChange = { onAction(BookingInboxAction.ReasonNoteChanged(it)) },
            onConfirm = { onAction(BookingInboxAction.ConfirmReason) },
        ),
    )
    TextButton(onClick = { onAction(BookingInboxAction.BackClicked) }, enabled = !isSubmitting) {
        Text("Volver al detalle")
    }
}
