package corp.khin.solutions.booqi.feature.provider

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import org.koin.compose.viewmodel.koinViewModel

/**
 * P4 — "Mis servicios", for `Destination.ServiceList`
 * (`corp.khin.solutions.booqi.core.navigation.Destination`). Navigation is exposed as callbacks so
 * Architect's wiring can map them to `Navigator` calls without this module importing another
 * feature: [onAddService] -> `Destination.ServiceEditor(serviceId = null)`, [onEditService] ->
 * `Destination.ServiceEditor(serviceId = id)`.
 */
@Suppress("ForbiddenComment") // Not surfacing errors via snackbar is deliberately deferred
// scaffolding, matching ProviderProfileScreen/BrowseScreen (no snackbar host in core:designsystem
// yet). Errors are still visible inline through ServiceListUiState.error.
@Composable
fun ServiceListScreen(
    onAddService: () -> Unit,
    onEditService: (serviceId: String) -> Unit,
    viewModel: ServiceListViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()

    // Reload on every entry: the ViewModel instance outlives this composition (see
    // ServiceListViewModel), so a service added/edited in the editor must be picked up here.
    LaunchedEffect(Unit) { viewModel.onAction(ServiceListAction.Refresh) }

    // Effects are collected once, separately from state — see ServiceListEvent for why.
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                ServiceListEvent.NavigateToAddService -> onAddService()
                is ServiceListEvent.NavigateToEditService -> onEditService(event.serviceId)
                // TODO surface via snackbar once core:designsystem has one
                is ServiceListEvent.ShowError -> Unit
            }
        }
    }

    ServiceListContent(state = state, onAction = viewModel::onAction)
}
