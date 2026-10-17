package corp.khin.solutions.booqi.feature.booking

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import corp.khin.solutions.booqi.core.designsystem.component.EmptyState
import corp.khin.solutions.booqi.core.designsystem.theme.BooqiSpacing

private const val LOAD_ERROR = "No pudimos cargar tus reservas"

/**
 * C11: the "Mis reservas" heading (visible in every state) and, once loaded, the four tabs over the
 * list. The body is a spinner, the visitor's sign-in prompt, a load failure with retry, the "sin
 * reservas" empty state, or the selected tab's list.
 */
@Composable
internal fun MyBookingsContent(state: MyBookingsUiState, onAction: (MyBookingsAction) -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "Mis reservas",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(start = BooqiSpacing.md, top = BooqiSpacing.md, end = BooqiSpacing.md),
        )
        Box(modifier = Modifier.weight(1f)) {
            when {
                state.isLoading -> LoadingContent()
                state.needsSignIn -> EmptyState(
                    title = "Inicia sesión para ver tus reservas",
                    description = "Tus solicitudes y citas aparecen aquí cuando tienes una cuenta.",
                    modifier = Modifier.fillMaxSize(),
                    action = {
                        Button(onClick = { onAction(MyBookingsAction.SignInClicked) }) { Text("Iniciar sesión") }
                    },
                )
                state.error != null -> MessageContent(
                    title = LOAD_ERROR,
                    message = state.error.describe(notFound = LOAD_ERROR),
                    actionLabel = "Reintentar",
                    onAction = { onAction(MyBookingsAction.Retry) },
                )
                state.hasNoBookings -> EmptyState(
                    title = "Aún no tienes reservas",
                    description = "Cuando solicites una cita, la verás aquí.",
                    modifier = Modifier.fillMaxSize(),
                    action = {
                        Button(onClick = { onAction(MyBookingsAction.ExploreClicked) }) { Text("Explorar servicios") }
                    },
                )
                else -> BookingsTabs(state = state, onAction = onAction)
            }
        }
    }
}

@Composable
private fun BookingsTabs(state: MyBookingsUiState, onAction: (MyBookingsAction) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        PrimaryTabRow(selectedTabIndex = BookingsTab.entries.indexOf(state.tab)) {
            BookingsTab.entries.forEach { tab ->
                Tab(
                    selected = state.tab == tab,
                    onClick = { onAction(MyBookingsAction.SelectTab(tab)) },
                    text = { Text(tab.label) },
                )
            }
        }
        BookingListContent(state = state, onAction = onAction)
    }
}
