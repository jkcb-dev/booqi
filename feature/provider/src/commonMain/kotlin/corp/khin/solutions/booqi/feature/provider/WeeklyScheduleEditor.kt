package corp.khin.solutions.booqi.feature.provider

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import corp.khin.solutions.booqi.core.designsystem.theme.BooqiSpacing
import corp.khin.solutions.booqi.core.designsystem.theme.LocalBooqiExtendedColors

/** The P6 tab body: a spinner while loading, a retry when loading failed, else the editor. */
@Composable
internal fun WeeklyScheduleContent(
    state: WeeklyScheduleUiState,
    onAction: (WeeklyScheduleAction) -> Unit,
) {
    when {
        state.isLoading -> CenteredColumn { CircularProgressIndicator() }
        state.loadError != null ->
            ScheduleLoadError(error = state.loadError, onRetry = { onAction(WeeklyScheduleAction.Start) })
        else -> WeeklyScheduleEditor(state = state, onAction = onAction)
    }
}

/** `WeeklyScheduleEditor` (docs/design/DESIGN_SYSTEM.md): seven schedule day rows, Monday to
 * Sunday, and the save button. A Provider with no schedule yet sees the same seven rows, all off
 * — there is no separate first-time state. */
@Composable
internal fun WeeklyScheduleEditor(
    state: WeeklyScheduleUiState,
    onAction: (WeeklyScheduleAction) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(BooqiSpacing.md),
        verticalArrangement = Arrangement.spacedBy(BooqiSpacing.sm),
    ) {
        Text(
            text = if (state.isScheduleDefined) {
                "Cambiá las horas de los días que quieras. Las citas ya aceptadas no se cancelan."
            } else {
                "Elegí los días que atendés y en qué horario. Todavía no definiste tu horario."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = LocalBooqiExtendedColors.current.ink2,
        )
        state.rows.forEach { row ->
            ScheduleDayRow(row = row, enabled = !state.isSaving, onAction = onAction)
        }
        state.formError?.let { message ->
            Text(text = message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
        }
        Button(
            onClick = { onAction(WeeklyScheduleAction.Save) },
            enabled = !state.isSaving && state.hasChanges,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (state.isSaving) "Guardando..." else "Guardar horario")
        }
        if (state.isSaved) {
            Text(
                text = "Horario guardado",
                style = MaterialTheme.typography.labelLarge,
                color = LocalBooqiExtendedColors.current.statusConfirmada,
            )
        }
    }
}
