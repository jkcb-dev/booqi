package corp.khin.solutions.booqi.feature.browse

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import org.koin.compose.viewmodel.koinViewModel

/**
 * C3 — a Service's detail, for `Destination.ProviderDetail(providerId)`
 * (`corp.khin.solutions.booqi.core.navigation.Destination`). Despite that destination's name its
 * parameter carries the **service id** (see its KDoc), which is [serviceId] here.
 *
 * Navigation is exposed as callbacks so Architect's wiring can map them to `Navigator` calls
 * without this module importing another feature: [onProviderSelected] (the Provider block) ->
 * `Destination.ProviderProfileView(providerId)`; [onBook] ("Reservar", with the Service and its
 * Provider) -> the booking flow of #26, **not built yet**; [onFinished] ("Volver") -> back.
 */
@Composable
fun ServiceDetailScreen(
    serviceId: String,
    onProviderSelected: (providerId: String) -> Unit,
    onBook: (serviceId: String, providerId: String) -> Unit,
    onFinished: () -> Unit,
    viewModel: ServiceDetailViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val currentOnProviderSelected by rememberUpdatedState(onProviderSelected)
    val currentOnBook by rememberUpdatedState(onBook)
    val currentOnFinished by rememberUpdatedState(onFinished)

    // Reset/reload on every entry and whenever serviceId changes: the ViewModel instance is
    // reused across visits (see ServiceDetailViewModel), so A -> provider -> B must never show A.
    LaunchedEffect(serviceId) { viewModel.onAction(ServiceDetailAction.Start(serviceId)) }

    // Effects are collected once, separately from state — see ServiceDetailEvent for why.
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is ServiceDetailEvent.NavigateToProvider -> currentOnProviderSelected(event.providerId)
                is ServiceDetailEvent.NavigateToBooking -> currentOnBook(event.serviceId, event.providerId)
                ServiceDetailEvent.NavigateBack -> currentOnFinished()
            }
        }
    }

    // Until Start has run for this id the state may still be another Service's: show loading.
    val shown = if (state.serviceId == serviceId) state else ServiceDetailUiState()
    ServiceDetailContent(state = shown, onAction = viewModel::onAction)
}
