@file:OptIn(ExperimentalMaterial3Api::class)

package corp.khin.solutions.booqi.feature.provider

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import corp.khin.solutions.booqi.core.designsystem.theme.BooqiCornerRadius
import corp.khin.solutions.booqi.core.designsystem.theme.BooqiSpacing
import corp.khin.solutions.booqi.core.designsystem.theme.LocalBooqiExtendedColors
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime

/** P3 — "pausar perfil": aviso explícito + selector de rango Desde/Hasta por fecha. Reached
 * in-screen from [ProfileSummaryContent] once the profile is complete. */
@Composable
internal fun PauseProfileSection(
    state: ProviderProfileUiState,
    onAction: (ProviderProfileAction) -> Unit,
) {
    var isFromPickerVisible by remember { mutableStateOf(false) }
    var isUntilPickerVisible by remember { mutableStateOf(false) }

    PauseProfileCard(
        state = state,
        onAction = onAction,
        onShowFromPicker = { isFromPickerVisible = true },
        onShowUntilPicker = { isUntilPickerVisible = true },
    )

    if (isFromPickerVisible) {
        PauseDatePickerDialog(
            initialDate = state.pauseFrom,
            onDismiss = { isFromPickerVisible = false },
            onConfirm = { onAction(ProviderProfileAction.PauseFromChanged(it)) },
        )
    }
    if (isUntilPickerVisible) {
        PauseDatePickerDialog(
            initialDate = state.pauseUntil,
            onDismiss = { isUntilPickerVisible = false },
            onConfirm = { onAction(ProviderProfileAction.PauseUntilChanged(it)) },
        )
    }
}

@Composable
private fun PauseProfileCard(
    state: ProviderProfileUiState,
    onAction: (ProviderProfileAction) -> Unit,
    onShowFromPicker: () -> Unit,
    onShowUntilPicker: () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(BooqiCornerRadius.medium),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(
            modifier = Modifier.padding(BooqiSpacing.md),
            verticalArrangement = Arrangement.spacedBy(BooqiSpacing.sm),
        ) {
            Text("Pausar perfil", style = MaterialTheme.typography.titleSmall)
            Text(
                text = "Las citas ya confirmadas no se cancelan.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )

            Row(horizontalArrangement = Arrangement.spacedBy(BooqiSpacing.sm)) {
                DateSelectorField(
                    label = "Desde",
                    date = state.pauseFrom,
                    onClick = onShowFromPicker,
                    modifier = Modifier.weight(1f),
                )
                DateSelectorField(
                    label = "Hasta",
                    date = state.pauseUntil,
                    onClick = onShowUntilPicker,
                    modifier = Modifier.weight(1f),
                )
            }

            state.pauseRangeError?.let { message ->
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(BooqiSpacing.sm)) {
                OutlinedButton(
                    onClick = { onAction(ProviderProfileAction.DismissPauseSheet) },
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Cancelar")
                }
                Button(
                    onClick = { onAction(ProviderProfileAction.ConfirmPause) },
                    enabled = !state.isPausing,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(if (state.isPausing) "Pausando..." else "Confirmar")
                }
            }
        }
    }
}

@Composable
private fun DateSelectorField(
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
private fun PauseDatePickerDialog(
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

private fun LocalDate.toUtcEpochMillis(): Long = atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds()

private fun Long.toLocalDateFromUtcEpochMillis(): LocalDate =
    Instant.fromEpochMilliseconds(this).toLocalDateTime(TimeZone.UTC).date
