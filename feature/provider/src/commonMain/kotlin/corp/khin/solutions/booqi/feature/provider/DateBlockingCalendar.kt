package corp.khin.solutions.booqi.feature.provider

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import corp.khin.solutions.booqi.core.designsystem.theme.BooqiCornerRadius
import corp.khin.solutions.booqi.core.designsystem.theme.BooqiSpacing
import corp.khin.solutions.booqi.core.designsystem.theme.LocalBooqiExtendedColors
import kotlinx.datetime.DayOfWeek

/**
 * `DateBlockingCalendar` (docs/design/DESIGN_SYSTEM.md): a monthly grid, Monday first, with month
 * navigation and a legend of how dates are painted. Tapping a date blocks/unblocks that whole day
 * ([DateBlockingAction.DateTapped]); today and the blocked dates are told apart in
 * [CalendarDayCell].
 */
@Composable
internal fun DateBlockingCalendar(
    state: DateBlockingUiState,
    onAction: (DateBlockingAction) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(BooqiSpacing.xs)) {
        MonthHeader(state = state, onAction = onAction)
        WeekdayHeader()
        monthWeeks(state.visibleMonth).forEach { week ->
            Row(modifier = Modifier.fillMaxWidth()) {
                week.forEach { date ->
                    if (date == null) {
                        Box(modifier = Modifier.weight(1f))
                    } else {
                        CalendarDayCell(
                            date = date,
                            blockState = state.blockedPeriods.blockStateOf(date),
                            isToday = date == state.today,
                            onClick = if (state.isUpdating) null else {
                                { onAction(DateBlockingAction.DateTapped(date)) }
                            },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
        CalendarLegend()
    }
}

@Composable
private fun MonthHeader(state: DateBlockingUiState, onAction: (DateBlockingAction) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        TextButton(
            onClick = { onAction(DateBlockingAction.PreviousMonth) },
            modifier = Modifier.semantics { contentDescription = "Mes anterior" },
        ) { Text("‹", style = MaterialTheme.typography.titleLarge) }
        Text(
            text = "${state.visibleMonth.month.spanishName()} ${state.visibleMonth.year}",
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f),
        )
        TextButton(
            onClick = { onAction(DateBlockingAction.NextMonth) },
            modifier = Modifier.semantics { contentDescription = "Mes siguiente" },
        ) { Text("›", style = MaterialTheme.typography.titleLarge) }
    }
}

@Composable
private fun WeekdayHeader() {
    Row(modifier = Modifier.fillMaxWidth()) {
        DayOfWeek.entries.forEach { day ->
            Text(
                text = day.spanishInitial(),
                style = MaterialTheme.typography.labelMedium,
                color = LocalBooqiExtendedColors.current.ink2,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CalendarLegend() {
    val scheme = MaterialTheme.colorScheme
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(BooqiSpacing.md),
        verticalArrangement = Arrangement.spacedBy(BooqiSpacing.xxs),
    ) {
        LegendItem(label = "Hoy") {
            Box(Modifier.size(BooqiSpacing.md).background(scheme.primary, CircleShape))
        }
        LegendItem(label = "Bloqueado") {
            Box(
                Modifier.size(BooqiSpacing.md)
                    .background(scheme.secondaryContainer, RoundedCornerShape(BooqiCornerRadius.small)),
            )
        }
        LegendItem(label = "Algunas horas") {
            Text("•", style = MaterialTheme.typography.labelSmall, color = scheme.secondary)
        }
    }
}

@Composable
private fun LegendItem(label: String, swatch: @Composable () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(BooqiSpacing.xs)) {
        swatch()
        Text(label, style = MaterialTheme.typography.labelSmall, color = LocalBooqiExtendedColors.current.ink2)
    }
}
