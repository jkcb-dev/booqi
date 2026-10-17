package corp.khin.solutions.booqi.feature.booking

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import org.koin.compose.viewmodel.koinViewModel

/**
 * C11 — "Mis reservas", for `Destination.MyBookings`. Callbacks, so no other feature is imported:
 * [onBookingSelected] (the booking id) opens its detail (`BookingDetailScreen`); [onSignInRequired]
 * is the visitor's "Iniciar sesión"; [onExplore] is the empty state's "Explorar servicios". It is a
 * top-level tab, so it has no back button.
 */
@Composable
fun MyBookingsScreen(
    onBookingSelected: (bookingId: String) -> Unit,
    onSignInRequired: () -> Unit,
    onExplore: () -> Unit,
    viewModel: MyBookingsViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val currentOnBookingSelected by rememberUpdatedState(onBookingSelected)
    val currentOnSignInRequired by rememberUpdatedState(onSignInRequired)
    val currentOnExplore by rememberUpdatedState(onExplore)

    // Reload on every entry: the ViewModel instance outlives this composition (see
    // MyBookingsViewModel), so a booking requested or answered since must be picked up here.
    LaunchedEffect(Unit) { viewModel.onAction(MyBookingsAction.Start) }

    // Effects are collected once, separately from state — see MyBookingsEvent for why.
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is MyBookingsEvent.BookingSelected -> currentOnBookingSelected(event.bookingId)
                MyBookingsEvent.SignInRequired -> currentOnSignInRequired()
                MyBookingsEvent.Explore -> currentOnExplore()
            }
        }
    }

    MyBookingsContent(state = state, onAction = viewModel::onAction)
}
