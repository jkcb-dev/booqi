package corp.khin.solutions.booqi.feature.provider

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import corp.khin.solutions.booqi.core.common.DomainError
import corp.khin.solutions.booqi.core.designsystem.theme.BooqiSpacing
import corp.khin.solutions.booqi.core.designsystem.theme.LocalBooqiExtendedColors

private const val LOAD_ERROR_MESSAGE = "No pudimos cargar tus servicios"

/** P4 body: loading spinner, load-failure retry, empty state, or the list (disabled services
 * included) with the "Agregar servicio" action. */
@Composable
internal fun ServiceListContent(
    state: ServiceListUiState,
    onAction: (ServiceListAction) -> Unit,
) {
    when {
        state.isLoading -> CenteredColumn { CircularProgressIndicator() }
        state.error != null && state.services.isEmpty() -> LoadErrorContent(state.error, onAction)
        state.services.isEmpty() -> EmptyServicesContent(onAction)
        else -> ServicesColumn(state, onAction)
    }
}

@Composable
private fun ServicesColumn(state: ServiceListUiState, onAction: (ServiceListAction) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(BooqiSpacing.md),
        verticalArrangement = Arrangement.spacedBy(BooqiSpacing.md),
    ) {
        item { Text("Mis servicios", style = MaterialTheme.typography.titleLarge) }
        state.error?.let { error ->
            item {
                Text(
                    text = error.describe(notFound = "No se encontró el servicio"),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
        items(state.services, key = { it.id }) { service ->
            ServiceListItem(
                service = service,
                isToggling = service.id in state.togglingServiceIds,
                onAction = onAction,
            )
        }
        item {
            Button(
                onClick = { onAction(ServiceListAction.AddServiceClicked) },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Agregar servicio") }
        }
    }
}

@Composable
private fun EmptyServicesContent(onAction: (ServiceListAction) -> Unit) {
    CenteredColumn {
        Text("Todavía no tenés servicios", style = MaterialTheme.typography.titleMedium)
        Text(
            text = "Agregá el primero para que los Clientes puedan encontrarte.",
            style = MaterialTheme.typography.bodyMedium,
            color = LocalBooqiExtendedColors.current.ink2,
            textAlign = TextAlign.Center,
        )
        Button(onClick = { onAction(ServiceListAction.AddServiceClicked) }) { Text("Agregar servicio") }
    }
}

@Composable
private fun LoadErrorContent(error: DomainError, onAction: (ServiceListAction) -> Unit) {
    CenteredColumn {
        Text(LOAD_ERROR_MESSAGE, style = MaterialTheme.typography.titleMedium)
        Text(
            text = error.describe(notFound = LOAD_ERROR_MESSAGE),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center,
        )
        OutlinedButton(onClick = { onAction(ServiceListAction.Refresh) }) { Text("Reintentar") }
    }
}

@Composable
internal fun CenteredColumn(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(BooqiSpacing.md),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(BooqiSpacing.sm, Alignment.CenterVertically),
    ) { content() }
}
