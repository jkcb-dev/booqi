package corp.khin.solutions.booqi.feature.booking

import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import corp.khin.solutions.booqi.core.designsystem.component.ReasonPicker
import corp.khin.solutions.booqi.core.designsystem.component.ReasonPickerCallbacks
import corp.khin.solutions.booqi.core.designsystem.component.ReasonPickerLabels
import corp.khin.solutions.booqi.core.designsystem.component.ReasonPickerState

/** The shared `ReasonPicker` with the Customer's cancellation reasons ("Otro" reveals the optional
 * note), plus a way back to the detail without acting. */
@Composable
internal fun CancelSection(
    form: CancelFormState,
    isCancelling: Boolean,
    onAction: (BookingDetailAction) -> Unit,
) {
    ReasonPicker(
        state = ReasonPickerState(
            options = customerReasonOptions,
            selectedId = form.code?.name,
            note = form.note,
            error = form.error,
            isSubmitting = isCancelling,
        ),
        labels = ReasonPickerLabels(
            title = "Motivo de la cancelación",
            noteLabel = "Detalle (opcional)",
            confirmLabel = "Cancelar cita",
        ),
        callbacks = ReasonPickerCallbacks(
            onSelect = { id -> customerReasonFor(id)?.let { onAction(BookingDetailAction.ReasonSelected(it)) } },
            onNoteChange = { onAction(BookingDetailAction.ReasonNoteChanged(it)) },
            onConfirm = { onAction(BookingDetailAction.ConfirmCancel) },
        ),
    )
    TextButton(onClick = { onAction(BookingDetailAction.CloseCancelForm) }, enabled = !isCancelling) {
        Text("Mantener mi cita")
    }
}
