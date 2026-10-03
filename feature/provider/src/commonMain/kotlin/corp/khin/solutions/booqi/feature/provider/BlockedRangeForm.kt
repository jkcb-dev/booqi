package corp.khin.solutions.booqi.feature.provider

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import corp.khin.solutions.booqi.core.designsystem.theme.BooqiCornerRadius
import corp.khin.solutions.booqi.core.designsystem.theme.BooqiSpacing

/** The "Bloquear horas" form of P7: a date (same picker as the P3 pause range) and a start/end
 * time within it. [BlockRangeFormState.error] — unparseable time, or "la hora de fin debe ser
 * posterior" from the use case — shows under the two time fields. */
@Composable
internal fun BlockedRangeForm(
    form: BlockRangeFormState,
    isUpdating: Boolean,
    onAction: (DateBlockingAction) -> Unit,
) {
    var isDatePickerVisible by remember { mutableStateOf(false) }
    Card(
        shape = RoundedCornerShape(BooqiCornerRadius.medium),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(
            modifier = Modifier.padding(BooqiSpacing.md),
            verticalArrangement = Arrangement.spacedBy(BooqiSpacing.sm),
        ) {
            Text("Bloquear horas", style = MaterialTheme.typography.titleSmall)
            DateSelectorField(label = "Fecha", date = form.date, onClick = { isDatePickerVisible = true })
            Row(horizontalArrangement = Arrangement.spacedBy(BooqiSpacing.sm)) {
                ScheduleTimeField(
                    value = form.startInput,
                    onValueChange = { onAction(DateBlockingAction.RangeStartChanged(it)) },
                    label = "Desde",
                    modifier = Modifier.weight(1f),
                    isError = form.error != null,
                )
                ScheduleTimeField(
                    value = form.endInput,
                    onValueChange = { onAction(DateBlockingAction.RangeEndChanged(it)) },
                    label = "Hasta",
                    modifier = Modifier.weight(1f),
                    isError = form.error != null,
                )
            }
            form.error?.let { message ->
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(BooqiSpacing.sm)) {
                OutlinedButton(
                    onClick = { onAction(DateBlockingAction.DismissRangeForm) },
                    modifier = Modifier.weight(1f),
                ) { Text("Cancelar") }
                Button(
                    onClick = { onAction(DateBlockingAction.ConfirmRange) },
                    enabled = !isUpdating,
                    modifier = Modifier.weight(1f),
                ) { Text(if (isUpdating) "Bloqueando..." else "Bloquear") }
            }
        }
    }
    if (isDatePickerVisible) {
        DateSelectionDialog(
            initialDate = form.date,
            onDismiss = { isDatePickerVisible = false },
            onConfirm = { onAction(DateBlockingAction.RangeDateChanged(it)) },
        )
    }
}
