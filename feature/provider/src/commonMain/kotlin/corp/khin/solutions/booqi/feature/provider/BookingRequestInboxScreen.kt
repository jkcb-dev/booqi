package corp.khin.solutions.booqi.feature.provider

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import corp.khin.solutions.booqi.core.designsystem.theme.BooqiSpacing
import corp.khin.solutions.booqi.domain.model.BookingStatus
import kotlinx.coroutines.delay
import org.koin.compose.viewmodel.koinViewModel

/** How often the screen re-reads the clock so "Marcar como completada" enables itself. */
private const val TICK_MILLIS = 30_000L

/**
 * P8–P10 — "Reservas", for `Destination.BookingRequestInbox`
 * (`corp.khin.solutions.booqi.core.navigation.Destination`): one screen with two tabs
 * ("Solicitudes" and "Confirmadas") and the request / confirmed-appointment detail as an in-screen
 * state, so no further destinations are needed. [onFinished] is called by the "Volver" button on
 * the lists — wiring typically maps it to a pop back to the profile; inside a detail (or an open
 * reason picker) "Volver" steps back within the screen instead.
 */
@Composable
fun BookingRequestInboxScreen(
    onFinished: () -> Unit = {},
    viewModel: BookingInboxViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()

    // Reload on every entry: the ViewModel instance outlives this composition (see
    // BookingInboxViewModel), so a request answered or added since must be picked up here.
    LaunchedEffect(Unit) { viewModel.onAction(BookingInboxAction.Start) }

    // Keeps the end-of-appointment rule current while the Provider stays on the screen.
    LaunchedEffect(Unit) {
        while (true) {
            delay(TICK_MILLIS)
            viewModel.onAction(BookingInboxAction.Tick)
        }
    }

    // Effects are collected once, separately from state — see BookingInboxEvent for why.
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                BookingInboxEvent.Finished -> onFinished()
            }
        }
    }

    BookingRequestInboxContent(state = state, onAction = viewModel::onAction)
}

private val TABS = listOf(BookingInboxTab.REQUESTS to "Solicitudes", BookingInboxTab.CONFIRMED to "Confirmadas")

/** The heading and "Volver" (visible in every state) over either the two tabs or the opened
 * booking's detail. */
@Composable
internal fun BookingRequestInboxContent(
    state: BookingInboxUiState,
    onAction: (BookingInboxAction) -> Unit,
) {
    val detail = state.detail
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = BooqiSpacing.md, top = BooqiSpacing.md, end = BooqiSpacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = detailTitle(detail),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = { onAction(BookingInboxAction.BackClicked) }) { Text("Volver") }
        }
        if (detail == null) {
            BookingTabs(state = state, onAction = onAction)
        } else {
            BookingDetailContent(state = state, detail = detail, onAction = onAction)
        }
    }
}

private fun detailTitle(detail: BookingDetailUiState?): String = when (detail?.booking?.status) {
    null -> "Reservas"
    BookingStatus.REQUESTED -> "Solicitud de reserva"
    BookingStatus.CONFIRMED -> "Cita confirmada"
    else -> "Detalle de la reserva"
}

@Composable
private fun ColumnScope.BookingTabs(state: BookingInboxUiState, onAction: (BookingInboxAction) -> Unit) {
    PrimaryTabRow(selectedTabIndex = TABS.indexOfFirst { it.first == state.tab }) {
        TABS.forEach { (tab, title) ->
            Tab(
                selected = state.tab == tab,
                onClick = { onAction(BookingInboxAction.SelectTab(tab)) },
                text = { Text(title) },
            )
        }
    }
    state.notice?.let { NoticeBanner(notice = it, onDismiss = { onAction(BookingInboxAction.DismissNotice) }) }
    Column(modifier = Modifier.weight(1f)) {
        BookingListContent(state = state, onAction = onAction)
    }
}
