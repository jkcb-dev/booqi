package corp.khin.solutions.booqi.feature.booking

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import kotlinx.coroutines.delay
import org.koin.compose.viewmodel.koinViewModel

/** How often the screen re-reads the clock so the countdowns and the 3h rule stay current. */
private const val TICK_MILLIS = 30_000L

/**
 * C8-C10b (and the completed / cancelled variants) — one Booking, for the booking-confirmation /
 * detail destination (see "For Architect" in the PR: it must carry the booking id). The variant is
 * decided by the Booking's status: pending (C8), confirmed (C9, with "Cancelar cita"), rejected
 * (C10) or expired (C10b), completed or cancelled. Callbacks, so no other feature is imported:
 * - [onRebook] (the Service id): "Buscar otro horario" — open the request flow for that Service;
 * - [onGoHome]: "Volver al inicio";
 * - [onRate] (the booking id): "Calificar" on a completed, not yet rated one — open C12;
 * - [onFinished]: "Volver".
 */
@Composable
fun BookingDetailScreen(
    bookingId: String,
    onRebook: (serviceId: String) -> Unit,
    onGoHome: () -> Unit,
    onRate: (bookingId: String) -> Unit,
    onFinished: () -> Unit,
) {
    val viewModel: BookingDetailViewModel = koinViewModel()
    val state by viewModel.state.collectAsState()
    val currentOnRebook by rememberUpdatedState(onRebook)
    val currentOnGoHome by rememberUpdatedState(onGoHome)
    val currentOnRate by rememberUpdatedState(onRate)
    val currentOnFinished by rememberUpdatedState(onFinished)

    // Reload on every entry and whenever bookingId changes: the ViewModel instance is reused across
    // visits (see BookingDetailViewModel), and the Provider may have answered meanwhile.
    LaunchedEffect(bookingId) { viewModel.onAction(BookingDetailAction.Start(bookingId)) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(TICK_MILLIS)
            viewModel.onAction(BookingDetailAction.Tick)
        }
    }

    // Effects are collected once, separately from state — see BookingDetailEvent for why.
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is BookingDetailEvent.Rebook -> currentOnRebook(event.serviceId)
                is BookingDetailEvent.Rate -> currentOnRate(event.bookingId)
                BookingDetailEvent.GoHome -> currentOnGoHome()
                BookingDetailEvent.Finished -> currentOnFinished()
            }
        }
    }

    // Until Start has run for this id the state may still be another Booking's: show loading.
    val shown = if (state.bookingId == bookingId) state else BookingDetailUiState(state.now, state.timeZone)
    BookingDetailContent(state = shown, onAction = viewModel::onAction)
}
