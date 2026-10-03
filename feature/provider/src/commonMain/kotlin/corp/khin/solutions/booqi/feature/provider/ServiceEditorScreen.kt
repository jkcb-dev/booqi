package corp.khin.solutions.booqi.feature.provider

import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.text.style.TextAlign
import corp.khin.solutions.booqi.core.common.DomainError
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * P5 — add/edit a service, for `Destination.ServiceEditor(serviceId)`
 * (`corp.khin.solutions.booqi.core.navigation.Destination`): [serviceId] `null` = add, non-null =
 * edit (form preloaded). [onFinished] is called after a successful save, and when the Provider
 * cancels or leaves a "servicio no encontrado" screen — wiring typically maps it to a pop back to
 * the service list.
 */
@Suppress("ForbiddenComment") // Not surfacing errors via snackbar is deliberately deferred
// scaffolding, matching ProviderProfileScreen/BrowseScreen (no snackbar host in core:designsystem
// yet). Form errors are rendered on their fields; other failures inline via ServiceEditorUiState.
@Composable
fun ServiceEditorScreen(
    serviceId: String?,
    onFinished: () -> Unit,
    viewModel: ServiceEditorViewModel = koinViewModel(
        key = serviceId ?: NEW_SERVICE_KEY,
        parameters = { parametersOf(serviceId) },
    ),
) {
    val state by viewModel.state.collectAsState()
    val currentOnFinished by rememberUpdatedState(onFinished)

    // Reset/reload on every entry and whenever serviceId changes: the ViewModel instance can be
    // reused across visits (see ServiceEditorViewModel), so add -> edit -> add and edit A -> edit B
    // must never show the previous visit's form.
    LaunchedEffect(serviceId) { viewModel.onAction(ServiceEditorAction.Start(serviceId)) }

    // Effects are collected once, separately from state — see ServiceEditorEvent for why.
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                ServiceEditorEvent.Saved -> currentOnFinished()
                // TODO surface via snackbar once core:designsystem has one
                is ServiceEditorEvent.ShowError -> Unit
            }
        }
    }

    when {
        state.isLoading -> CenteredColumn { CircularProgressIndicator() }
        state.error == DomainError.NotFound -> ServiceNotFoundContent(onFinished)
        else -> ServiceEditorFormContent(
            state = state,
            onAction = viewModel::onAction,
            onCancel = onFinished,
        )
    }
}

@Composable
private fun ServiceNotFoundContent(onBack: () -> Unit) {
    CenteredColumn {
        Text("Servicio no encontrado", style = MaterialTheme.typography.titleMedium)
        Text(
            text = "Es posible que ya no exista.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
        )
        OutlinedButton(onClick = onBack) { Text("Volver") }
    }
}

private const val NEW_SERVICE_KEY = "new-service"
