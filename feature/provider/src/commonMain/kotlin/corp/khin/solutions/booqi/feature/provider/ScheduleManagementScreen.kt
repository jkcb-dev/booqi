package corp.khin.solutions.booqi.feature.provider

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import corp.khin.solutions.booqi.core.designsystem.theme.BooqiSpacing
import org.koin.compose.viewmodel.koinViewModel

/**
 * P6 + P7 — "Mi horario", for `Destination.ScheduleManagement`
 * (`corp.khin.solutions.booqi.core.navigation.Destination`): one screen with two tabs, the
 * weekly schedule editor ([WeeklyScheduleViewModel]) and the date-blocking calendar
 * ([DateBlockingViewModel]). [onFinished] is called by the "Volver" button — wiring typically maps
 * it to a pop back to the profile.
 */
@Suppress("ForbiddenComment") // Not surfacing errors via snackbar is deliberately deferred
// scaffolding, matching ServiceListScreen/ProviderProfileScreen (no snackbar host in
// core:designsystem yet). Errors are still visible inline through each UiState.
@Composable
fun ScheduleManagementScreen(
    onFinished: () -> Unit = {},
    weeklyViewModel: WeeklyScheduleViewModel = koinViewModel(),
    blockingViewModel: DateBlockingViewModel = koinViewModel(),
) {
    val weeklyState by weeklyViewModel.state.collectAsState()
    val blockingState by blockingViewModel.state.collectAsState()

    // Reload both halves on every entry (and only then — not per tab switch, which would throw
    // away unsaved weekly edits): the ViewModel instances outlive this composition (see
    // docs/DEVELOPMENT.md), so a stale schedule/month must never be shown on re-entry.
    LaunchedEffect(Unit) {
        weeklyViewModel.onAction(WeeklyScheduleAction.Start)
        blockingViewModel.onAction(DateBlockingAction.Start)
    }

    // Effects are collected once, separately from state — see the *Event types for why.
    LaunchedEffect(Unit) {
        weeklyViewModel.events.collect { event ->
            when (event) {
                // TODO surface via snackbar once core:designsystem has one
                is WeeklyScheduleEvent.ShowError -> Unit
            }
        }
    }
    LaunchedEffect(Unit) {
        blockingViewModel.events.collect { event ->
            when (event) {
                // TODO surface via snackbar once core:designsystem has one
                is DateBlockingEvent.ShowError -> Unit
            }
        }
    }

    ScheduleManagementContent(
        weeklyState = weeklyState,
        blockingState = blockingState,
        onWeeklyAction = weeklyViewModel::onAction,
        onBlockingAction = blockingViewModel::onAction,
        onBack = onFinished,
    )
}

private val TAB_TITLES = listOf("Horario semanal", "Bloquear fechas")

/** The "Mi horario" heading (visible in every state) over the two tabs. */
@Composable
internal fun ScheduleManagementContent(
    weeklyState: WeeklyScheduleUiState,
    blockingState: DateBlockingUiState,
    onWeeklyAction: (WeeklyScheduleAction) -> Unit,
    onBlockingAction: (DateBlockingAction) -> Unit,
    onBack: () -> Unit,
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = BooqiSpacing.md, top = BooqiSpacing.md, end = BooqiSpacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Mi horario", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            TextButton(onClick = onBack) { Text("Volver") }
        }
        PrimaryTabRow(selectedTabIndex = selectedTab) {
            TAB_TITLES.forEachIndexed { index, title ->
                Tab(selected = selectedTab == index, onClick = { selectedTab = index }, text = { Text(title) })
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            if (selectedTab == 0) {
                WeeklyScheduleContent(state = weeklyState, onAction = onWeeklyAction)
            } else {
                DateBlockingContent(state = blockingState, onAction = onBlockingAction)
            }
        }
    }
}
