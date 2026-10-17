package corp.khin.solutions.booqi.feature.booking

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import corp.khin.solutions.booqi.core.common.DomainError
import org.koin.compose.viewmodel.koinViewModel

/**
 * C5 + C7 — request a reservation for a Service, for the booking destination (see "For Architect"
 * in the PR: `Destination.Booking` must carry the **service id**). Navigation and hand-offs are
 * callbacks so this module imports no other feature:
 * - [onSignInRequired]: no session / unverified email — open the sign-in or account screen; coming
 *   back to the same Service resumes the flow with the chosen slot;
 * - [onAddAddress]: a Domicilio request has no saved address — open the address screen (#23);
 * - [onDone] (the new pending booking's id): the request was sent — show C8 (`BookingDetailScreen`);
 * - [onFinished]: the Customer backed out of the flow.
 */
@Composable
fun BookingFlowScreen(
    serviceId: String,
    onSignInRequired: () -> Unit,
    onAddAddress: () -> Unit,
    onDone: (bookingId: String) -> Unit,
    onFinished: () -> Unit,
) {
    val viewModel: BookingFlowViewModel = koinViewModel()
    val state by viewModel.state.collectAsState()
    val currentOnSignInRequired by rememberUpdatedState(onSignInRequired)
    val currentOnAddAddress by rememberUpdatedState(onAddAddress)
    val currentOnDone by rememberUpdatedState(onDone)
    val currentOnFinished by rememberUpdatedState(onFinished)

    // Reset/reload on every entry and whenever serviceId changes: the ViewModel instance is reused
    // across visits (see BookingFlowViewModel), so A -> B must never show A's flow. Returning from
    // sign-in / the address screen to the same Service resumes instead.
    LaunchedEffect(serviceId) { viewModel.onAction(BookingFlowAction.Start(serviceId)) }

    // Effects are collected once, separately from state — see BookingFlowEvent for why.
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                BookingFlowEvent.SignInRequired -> currentOnSignInRequired()
                BookingFlowEvent.AddAddress -> currentOnAddAddress()
                is BookingFlowEvent.Done -> currentOnDone(event.bookingId)
                BookingFlowEvent.Finished -> currentOnFinished()
            }
        }
    }

    // Until Start has run for this id the state may still be another Service's: show loading.
    val shown = if (state.serviceId == serviceId) state else BookingFlowUiState.initial(state.today, serviceId)
    BookingFlowContent(state = shown, onAction = viewModel::onAction)
}

internal const val SERVICE_NOT_FOUND_TITLE = "Servicio no encontrado"
internal const val LOAD_ERROR_TITLE = "No pudimos cargar el servicio"

internal fun DomainError?.isNotFound(): Boolean = this == DomainError.NotFound
