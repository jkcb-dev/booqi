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
 * ViewModels), then [Destination.Booking] for the chosen Service. No session pushes the sign-in flow
 * and an unverified email the verification notice, so finishing either pops back.
 *
 * [returnToServiceId]: when the screen knows its Service (C3), the booking screen is pushed *under*
 * the sign-in/verification flow, so after signing in the visitor lands on `Booking(serviceId)`
 * instead of the detail. C4 does not know which Service was tapped when the guard refuses, so it
 * returns to the profile and the visitor taps "Reservar" again.
 */
private fun bookingNavigation(navigator: Navigator, returnToServiceId: String? = null): BookingNavigation {
    fun pushOverBooking(destination: Destination) {
        returnToServiceId?.let { navigator.navigateTo(Destination.Booking(it)) }
        navigator.navigateTo(destination)
    }
    return BookingNavigation(
        onBook = { serviceId, _ -> navigator.navigateTo(Destination.Booking(serviceId)) },
        onSignInRequired = { pushOverBooking(Destination.Auth()) },
        onVerificationRequired = { pushOverBooking(Destination.VerifyEmail) },
    )
}

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
            booking = bookingNavigation(navigator, returnToServiceId = destination.serviceId),
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
