package corp.khin.solutions.booqi.feature.booking

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import org.koin.compose.viewmodel.koinViewModel

/**
 * C12 — rate a completed appointment, for the rating destination (see "For Architect" in the PR).
 * [onFinished] is called when the rating was saved or the Customer backs out; wiring maps it to a
 * pop back to the booking detail.
 */
@Composable
fun RateBookingScreen(
    bookingId: String,
    onFinished: () -> Unit,
    viewModel: RateBookingViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val currentOnFinished by rememberUpdatedState(onFinished)

    // Reset/reload on every entry and whenever bookingId changes: the ViewModel instance is reused
    // across visits (see RateBookingViewModel), so rating B must never show A's stars.
    LaunchedEffect(bookingId) { viewModel.onAction(RateBookingAction.Start(bookingId)) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                RateBookingEvent.Finished -> currentOnFinished()
            }
        }
    }

    // Until Start has run for this id the state may still be another Booking's: show loading.
    val shown = if (state.bookingId == bookingId) state else RateBookingUiState()
    RateBookingContent(state = shown, onAction = viewModel::onAction)
}
