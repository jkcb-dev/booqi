package corp.khin.solutions.booqi.feature.provider

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import corp.khin.solutions.booqi.core.designsystem.theme.BooqiSpacing

/** The P7 tab body: a spinner while loading, a retry when loading failed, else the calendar. */
@Composable
internal fun DateBlockingContent(
    state: DateBlockingUiState,
    onAction: (DateBlockingAction) -> Unit,
) {
    when {
        state.isLoading -> CenteredColumn { CircularProgressIndicator() }
        state.loadError != null ->
            ScheduleLoadError(error = state.loadError, onRetry = { onAction(DateBlockingAction.Start) })
        else -> DateBlockingBody(state = state, onAction = onAction)
    }
}

@Composable
private fun DateBlockingBody(
    state: DateBlockingUiState,
    onAction: (DateBlockingAction) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(BooqiSpacing.md),
        verticalArrangement = Arrangement.spacedBy(BooqiSpacing.md),
    ) {
        state.actionError?.let { message ->
            Text(text = message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
        }
        DateBlockingCalendar(state = state, onAction = onAction)
        val form = state.rangeForm
        if (form != null) {
            BlockedRangeForm(form = form, isUpdating = state.isUpdating, onAction = onAction)
        } else {
            OutlinedButton(
                onClick = { onAction(DateBlockingAction.ShowRangeForm) },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Bloquear horas de un día") }
        }
        BlockedPeriodsList(periods = state.blockedPeriods, enabled = !state.isUpdating, onAction = onAction)
    }
}
