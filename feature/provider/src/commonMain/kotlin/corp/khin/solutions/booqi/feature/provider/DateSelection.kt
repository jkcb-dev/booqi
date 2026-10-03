@file:OptIn(ExperimentalMaterial3Api::class)

package corp.khin.solutions.booqi.feature.provider

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import corp.khin.solutions.booqi.core.designsystem.theme.BooqiSpacing
import corp.khin.solutions.booqi.core.designsystem.theme.LocalBooqiExtendedColors
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime

// Date selection shared by the P3 pause range (ProviderProfilePauseSection) and the P7 "bloquear
// horas" form (BlockedRangeForm): a labelled button that opens a Material3 DatePicker dialog.
// Extracted from the pause section in #17 instead of being copied.

@Composable
internal fun DateSelectorField(
    label: String,
    date: LocalDate?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(BooqiSpacing.xxs)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = LocalBooqiExtendedColors.current.ink2,
        )
        OutlinedButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
            Text(date?.toString() ?: "Seleccionar")
        }
    }
}

@Composable
internal fun DateSelectionDialog(
    initialDate: LocalDate?,
    onDismiss: () -> Unit,
    onConfirm: (LocalDate) -> Unit,
) {
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialDate?.toUtcEpochMillis(),
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        onConfirm(millis.toLocalDateFromUtcEpochMillis())
                    }
                    onDismiss()
                },
            ) { Text("Aceptar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        },
    ) {
        DatePicker(state = datePickerState)
    }
}

internal fun LocalDate.toUtcEpochMillis(): Long = atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds()

internal fun Long.toLocalDateFromUtcEpochMillis(): LocalDate =
    Instant.fromEpochMilliseconds(this).toLocalDateTime(TimeZone.UTC).date
