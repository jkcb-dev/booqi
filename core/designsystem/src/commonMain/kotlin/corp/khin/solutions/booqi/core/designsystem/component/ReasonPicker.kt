package corp.khin.solutions.booqi.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import corp.khin.solutions.booqi.core.designsystem.theme.BooqiSpacing

/**
 * One selectable reason. [id] is whatever the owning feature uses to identify it (the design
 * system never interprets it); [allowsNote] marks the "Otro" option, the only one that reveals the
 * free-text field.
 */
data class ReasonOption(
    val id: String,
    val label: String,
    val allowsNote: Boolean = false,
)

/** What the picker shows and enables. [error] renders under the options (e.g. "Elige un motivo"). */
data class ReasonPickerState(
    val options: List<ReasonOption>,
    val selectedId: String?,
    val note: String,
    val error: String? = null,
    val isSubmitting: Boolean = false,
)

/** All user-facing copy, supplied by the caller so the same picker serves reject and cancel. */
data class ReasonPickerLabels(
    val title: String,
    val noteLabel: String,
    val confirmLabel: String,
)

/** The picker's outputs; the caller owns the state and reacts to these. */
data class ReasonPickerCallbacks(
    val onSelect: (id: String) -> Unit,
    val onNoteChange: (String) -> Unit,
    val onConfirm: () -> Unit,
)

/**
 * Reason picker organism (docs/design/DESIGN_SYSTEM.md — list of reason options + optional free
 * text for "Otro" + confirm button; Figma P9 reject, and the Provider's cancel-accepted-appointment
 * flow). Lives in `core:designsystem` because the Customer side reuses the same shape for its own
 * cancellation list (#25). It takes primitives only ([ReasonOption] carries a String id and label)
 * so the design system never depends on `domain`; each feature maps its own reason codes and
 * wording.
 *
 * The confirm button stays disabled until an option is selected (a reason is required) or while
 * [ReasonPickerState.isSubmitting]; the free-text field is optional and only shown for an option
 * with [ReasonOption.allowsNote]. A read-only variant (Customer's C10) is not built yet.
 */
@Composable
fun ReasonPicker(
    state: ReasonPickerState,
    labels: ReasonPickerLabels,
    callbacks: ReasonPickerCallbacks,
    modifier: Modifier = Modifier,
) {
    val noteVisible = state.options.any { it.id == state.selectedId && it.allowsNote }
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(BooqiSpacing.sm)) {
        Text(labels.title, style = MaterialTheme.typography.titleMedium)
        state.options.forEach { option ->
            ReasonOptionRow(
                option = option,
                selected = option.id == state.selectedId,
                enabled = !state.isSubmitting,
                onSelect = { callbacks.onSelect(option.id) },
            )
        }
        if (noteVisible) {
            OutlinedTextField(
                value = state.note,
                onValueChange = callbacks.onNoteChange,
                label = { Text(labels.noteLabel) },
                enabled = !state.isSubmitting,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        state.error?.let { error ->
            Text(error, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
        }
        Button(
            onClick = callbacks.onConfirm,
            enabled = state.selectedId != null && !state.isSubmitting,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error,
                contentColor = MaterialTheme.colorScheme.onError,
            ),
            modifier = Modifier.fillMaxWidth(),
        ) { Text(labels.confirmLabel) }
    }
}

/** The "Reason option" molecule: radio + label on one tappable row. */
@Composable
private fun ReasonOptionRow(
    option: ReasonOption,
    selected: Boolean,
    enabled: Boolean,
    onSelect: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, enabled = enabled, role = Role.RadioButton, onClick = onSelect),
    ) {
        RadioButton(selected = selected, onClick = null, enabled = enabled)
        Text(
            text = option.label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(start = BooqiSpacing.sm),
        )
    }
}
