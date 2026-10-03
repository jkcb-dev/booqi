package corp.khin.solutions.booqi.feature.browse

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import org.koin.compose.viewmodel.koinViewModel

/**
 * C4 — a Provider's public page, for `Destination.ProviderProfileView(providerId)`
 * (`corp.khin.solutions.booqi.core.navigation.Destination`). Named `ProviderPublicProfileScreen`
 * because `feature:provider` already has a `ProviderProfileScreen` (the Provider's own profile).
 *
 * Navigation is exposed as callbacks so Architect's wiring can map them to `Navigator` calls
 * without this module importing another feature: [onServiceSelected] (a Service card) ->
 * `Destination.ServiceDetail(serviceId)`, the Service detail; [onBook] ("Reservar ›", with the
 * Service and its Provider) -> the booking flow of #26, **not built yet**; [onFinished]
 * ("Volver") -> back.
 */
@Composable
fun ProviderPublicProfileScreen(
    providerId: String,
    onServiceSelected: (serviceId: String) -> Unit,
    onBook: (serviceId: String, providerId: String) -> Unit,
    onFinished: () -> Unit,
    viewModel: ProviderPublicProfileViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val currentOnServiceSelected by rememberUpdatedState(onServiceSelected)
    val currentOnBook by rememberUpdatedState(onBook)
    val currentOnFinished by rememberUpdatedState(onFinished)

    // Reset/reload on every entry and whenever providerId changes: the ViewModel instance is
    // reused across visits (see ProviderPublicProfileViewModel).
    LaunchedEffect(providerId) { viewModel.onAction(ProviderPublicProfileAction.Start(providerId)) }

    // Effects are collected once, separately from state — see ProviderPublicProfileEvent for why.
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is ProviderPublicProfileEvent.NavigateToService -> currentOnServiceSelected(event.serviceId)
                is ProviderPublicProfileEvent.NavigateToBooking -> currentOnBook(event.serviceId, event.providerId)
                ProviderPublicProfileEvent.NavigateBack -> currentOnFinished()
            }
        }
    }

    // Until Start has run for this id the state may still be another Provider's: show loading.
    val shown = if (state.providerId == providerId) state else ProviderPublicProfileUiState()
    ProviderPublicProfileContent(state = shown, onAction = viewModel::onAction)
}
