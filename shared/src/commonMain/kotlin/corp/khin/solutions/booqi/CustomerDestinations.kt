package corp.khin.solutions.booqi

import androidx.compose.runtime.Composable
import corp.khin.solutions.booqi.core.navigation.Destination
import corp.khin.solutions.booqi.core.navigation.Navigator
import corp.khin.solutions.booqi.feature.browse.BookingNavigation
import corp.khin.solutions.booqi.feature.browse.BrowseScreen
import corp.khin.solutions.booqi.feature.browse.ProviderPublicProfileScreen
import corp.khin.solutions.booqi.feature.browse.ServiceDetailScreen

/**
 * "Reservar" from C3/C4: the access guard runs first (`RequerirCuentaVerificada`, inside the browse
 * ViewModels). No session pushes the sign-in flow over the current screen and an unverified email the
 * verification notice, so finishing either pops back to where the visitor was.
 */
private fun bookingNavigation(navigator: Navigator) = BookingNavigation(
    // TEMPORARY no-op: the Customer booking flow is #26 (feature:booking not built yet). The guard
    // above already runs, so only a verified account gets this far.
    onBook = { _, _ -> },
    onSignInRequired = { navigator.navigateTo(Destination.Auth()) },
    onVerificationRequired = { navigator.navigateTo(Destination.VerifyEmail) },
)

/** `feature:browse` — the public catalog; no account needed to explore it. */
@Composable
internal fun CustomerDestination(destination: Destination, navigator: Navigator) {
    when (destination) {
        is Destination.Browse -> BrowseScreen(
            onProviderSelected = { serviceId ->
                navigator.navigateTo(Destination.ServiceDetail(serviceId))
            },
        )
        is Destination.ServiceDetail -> ServiceDetailScreen(
            serviceId = destination.serviceId,
            onProviderSelected = { id -> navigator.navigateTo(Destination.ProviderProfileView(id)) },
            booking = bookingNavigation(navigator),
            onFinished = { navigator.backOr(Destination.Browse) },
        )
        is Destination.ProviderProfileView -> ProviderPublicProfileScreen(
            providerId = destination.providerId,
            onServiceSelected = { id -> navigator.navigateTo(Destination.ServiceDetail(id)) },
            booking = bookingNavigation(navigator),
            onFinished = { navigator.backOr(Destination.Browse) },
        )
        else -> Unit
    }
}
