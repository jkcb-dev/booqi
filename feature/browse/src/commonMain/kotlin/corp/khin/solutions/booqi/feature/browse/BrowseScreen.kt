package corp.khin.solutions.booqi.feature.browse

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import corp.khin.solutions.booqi.domain.model.ServiceProvider
import org.koin.compose.viewmodel.koinViewModel

private const val CENTS_PER_UNIT = 100

@Suppress("ForbiddenComment") // Not surfacing errors visually yet is deliberately deferred
// scaffolding, not a bug — there's no ticket system for this project yet to file it against
// instead (see the roles conversation this is tracked in).
@Composable
fun BrowseScreen(
    onProviderSelected: (String) -> Unit,
    viewModel: BrowseViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()

    // Effects are collected once, separately from state — see BrowseEvent for why.
    androidx.compose.runtime.LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is BrowseEvent.NavigateToProviderDetail -> onProviderSelected(event.providerId)
                // TODO surface via snackbar once core:designsystem has one
                is BrowseEvent.ShowError -> Unit
            }
        }
    }

    BrowseContent(state = state, onAction = viewModel::onAction)
}

@Composable
private fun BrowseContent(
    state: BrowseUiState,
    onAction: (BrowseAction) -> Unit,
) {
    when {
        state.isLoading -> Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) { CircularProgressIndicator() }

        else -> LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(state.providers, key = { it.id }) { provider ->
                ProviderCard(provider = provider, onClick = { onAction(BrowseAction.SelectProvider(provider)) })
            }
        }
    }
}

@Composable
private fun ProviderCard(provider: ServiceProvider, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxSize().padding(0.dp),
        onClick = onClick,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(provider.name)
            Text(provider.shortTagline)
            Text("★ ${provider.ratingOutOf5} · from ${provider.priceFromCents / CENTS_PER_UNIT}")
        }
    }
}
