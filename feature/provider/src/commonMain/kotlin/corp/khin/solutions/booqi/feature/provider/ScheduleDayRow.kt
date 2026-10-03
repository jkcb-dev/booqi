package corp.khin.solutions.booqi.feature.provider

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import corp.khin.solutions.booqi.core.designsystem.theme.BooqiCornerRadius
import corp.khin.solutions.booqi.core.designsystem.theme.BooqiSpacing
import corp.khin.solutions.booqi.core.designsystem.theme.LocalBooqiExtendedColors

/** One P6 "schedule day row": the day's name, its on/off toggle, and — while the day is on — the
 * start/end time fields. An error (unparseable time, or the use case's "end after start" rule)
 * renders under the fields. [enabled] (the toggle) is false while a save is in flight; edits to
 * the time fields are ignored by the ViewModel meanwhile. */
@Composable
internal fun ScheduleDayRow(
    row: ScheduleDayRowState,
    enabled: Boolean,
    onAction: (WeeklyScheduleAction) -> Unit,
) {
    val ink2 = LocalBooqiExtendedColors.current.ink2
    Card(
        shape = RoundedCornerShape(BooqiCornerRadius.medium),
        colors = CardDefaults.cardColors(
            containerColor = if (row.isActive) {
                MaterialTheme.colorScheme.surface
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(BooqiSpacing.md),
            verticalArrangement = Arrangement.spacedBy(BooqiSpacing.xs),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = row.day.spanishName(),
                    style = MaterialTheme.typography.titleMedium,
                    color = if (row.isActive) MaterialTheme.colorScheme.onSurface else ink2,
                    modifier = Modifier.weight(1f),
                )
                Switch(
                    checked = row.isActive,
                    onCheckedChange = { onAction(WeeklyScheduleAction.DayToggled(row.day, it)) },
                    enabled = enabled,
                )
            }
            if (row.isActive) {
                DayHoursFields(row = row, onAction = onAction)
            } else {
                Text("No disponible", style = MaterialTheme.typography.bodyMedium, color = ink2)
            }
        }
    }
}

@Composable
private fun DayHoursFields(
    row: ScheduleDayRowState,
    onAction: (WeeklyScheduleAction) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(BooqiSpacing.sm)) {
        ScheduleTimeField(
            value = row.startInput,
            onValueChange = { onAction(WeeklyScheduleAction.StartChanged(row.day, it)) },
            label = "Desde",
            modifier = Modifier.weight(1f),
            isError = row.error != null,
        )
        ScheduleTimeField(
            value = row.endInput,
            onValueChange = { onAction(WeeklyScheduleAction.EndChanged(row.day, it)) },
            label = "Hasta",
            modifier = Modifier.weight(1f),
            isError = row.error != null,
        )
    }
    row.error?.let { message ->
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
        )
    }
}
