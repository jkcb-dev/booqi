package corp.khin.solutions.booqi.feature.browse

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import org.koin.compose.viewmodel.koinViewModel

/**
 * C1 + C2 — the Customer's search and results, for `Destination.Browse`
 * (`corp.khin.solutions.booqi.core.navigation.Destination`).
 *
 * [onProviderSelected] is called with the **service id** of the tapped result (the name predates
 * the model correction and is kept so `App.kt` keeps compiling): wiring maps it to
 * `Destination.ProviderDetail(serviceId)`, which despite its name renders a Service's detail.
 */
@Composable
fun BrowseScreen(
    onProviderSelected: (String) -> Unit,
    viewModel: BrowseViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val currentOnSelected by rememberUpdatedState(onProviderSelected)

    // Re-run the current search on every entry: the ViewModel instance outlives this composition
    // (see BrowseViewModel), so coming back from a detail must not show a stale list.
    LaunchedEffect(Unit) { viewModel.onAction(BrowseAction.Refresh) }

    // Effects are collected once, separately from state — see BrowseEvent for why.
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is BrowseEvent.NavigateToServiceDetail -> currentOnSelected(event.serviceId)
            }
        }
    }

    BrowseContent(state = state, onAction = viewModel::onAction)
}
