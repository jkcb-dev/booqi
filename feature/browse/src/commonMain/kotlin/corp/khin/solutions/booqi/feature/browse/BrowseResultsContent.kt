package corp.khin.solutions.booqi.feature.browse

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import corp.khin.solutions.booqi.core.designsystem.component.EmptyState
import corp.khin.solutions.booqi.core.designsystem.theme.BooqiSpacing

private const val SEARCH_ERROR_TITLE = "No pudimos buscar servicios"

/**
 * C2: the distance filter and the result counter over a body that is a spinner (first load), a
 * failure with "Reintentar", the "no encontramos" empty state, or the list. A refresh over an
 * already-shown list keeps the list on screen instead of blanking it with a spinner.
 */
@Composable
internal fun ResultsContent(state: BrowseUiState, onAction: (BrowseAction) -> Unit) {
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(BooqiSpacing.xs)) {
        DistanceFilter(radiusKm = state.radiusKm, onAction = onAction)
        ResultsHeader(state = state, onAction = onAction)
        Box(modifier = Modifier.weight(1f)) {
            when {
                state.isLoading && state.results.isEmpty() -> LoadingContent()
                state.error != null -> MessageContent(
                    title = SEARCH_ERROR_TITLE,
                    message = state.error.describe(notFound = SEARCH_ERROR_TITLE),
                    actionLabel = "Reintentar",
                    onAction = { onAction(BrowseAction.Refresh) },
                )
                state.results.isEmpty() -> EmptyState(
                    title = "No encontramos servicios",
                    description = "Probá con otras palabras, otra categoría o una distancia mayor.",
                    modifier = Modifier.fillMaxSize(),
                    action = {
                        OutlinedButton(onClick = { onAction(BrowseAction.ClearSearch) }) { Text("Limpiar búsqueda") }
                    },
                )
                else -> ResultsList(state = state, onAction = onAction)
            }
        }
    }
}

/** The counter ("3 resultados", "Buscando…") and the way back to C1. */
@Composable
private fun ResultsHeader(state: BrowseUiState, onAction: (BrowseAction) -> Unit) {
    val counter = when {
        state.isLoading && state.results.isEmpty() -> "Buscando…"
        state.error != null -> ""
        else -> resultCountLabel(state.resultCount)
    }
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = BooqiSpacing.md, end = BooqiSpacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = counter,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f),
        )
        TextButton(onClick = { onAction(BrowseAction.ClearSearch) }) { Text("Limpiar") }
    }
}

@Composable
private fun ResultsList(state: BrowseUiState, onAction: (BrowseAction) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = BooqiSpacing.md, end = BooqiSpacing.md, bottom = BooqiSpacing.md),
        verticalArrangement = Arrangement.spacedBy(BooqiSpacing.sm),
    ) {
        items(state.results, key = { it.service.id }) { result ->
            ProviderCard(result = result, onClick = { onAction(BrowseAction.ServiceSelected(result.service.id)) })
        }
    }
}
