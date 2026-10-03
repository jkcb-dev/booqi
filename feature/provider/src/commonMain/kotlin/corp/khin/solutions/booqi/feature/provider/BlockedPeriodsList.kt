package corp.khin.solutions.booqi.feature.provider

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import corp.khin.solutions.booqi.core.designsystem.theme.BooqiSpacing
import corp.khin.solutions.booqi.core.designsystem.theme.LocalBooqiExtendedColors
import corp.khin.solutions.booqi.domain.model.BlockedPeriod

/** "Fechas bloqueadas": every blocked period in the domain's chronological order, each with a
 * "Desbloquear" — the only way to free a blocked *time range* (a calendar tap only toggles the
 * whole day). */
@Composable
internal fun BlockedPeriodsList(
    periods: List<BlockedPeriod>,
    enabled: Boolean,
    onAction: (DateBlockingAction) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(BooqiSpacing.xxs)) {
        Text("Fechas bloqueadas", style = MaterialTheme.typography.titleSmall)
        if (periods.isEmpty()) {
            Text(
                text = "No tenés fechas bloqueadas. Tocá un día del calendario para bloquearlo.",
                style = MaterialTheme.typography.bodyMedium,
                color = LocalBooqiExtendedColors.current.ink2,
            )
        }
        periods.forEach { period ->
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(period.date.spanishText(), style = MaterialTheme.typography.bodyMedium)
                    Text(
                        text = period.timeRange?.let { "${formatTime(it.start)} – ${formatTime(it.end)}" }
                            ?: "Todo el día",
                        style = MaterialTheme.typography.bodySmall,
                        color = LocalBooqiExtendedColors.current.ink2,
                    )
                }
                TextButton(
                    onClick = { onAction(DateBlockingAction.Unblock(period)) },
                    enabled = enabled,
                ) { Text("Desbloquear") }
            }
        }
    }
}
