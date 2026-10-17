package corp.khin.solutions.booqi

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import corp.khin.solutions.booqi.core.designsystem.theme.BooqiTheme
import corp.khin.solutions.booqi.core.navigation.DefaultNavigator
import corp.khin.solutions.booqi.core.navigation.Destination
import corp.khin.solutions.booqi.core.navigation.Navigator
import corp.khin.solutions.booqi.feature.browse.BrowseScreen

/**
 * App root: theme + the single [DefaultNavigator] instance for the whole app, the current
 * destination, and the bottom navigation of whichever session shell (Cliente or Proveedor) it
 * belongs to (see [AppNavigationBar]). Destinations are grouped by feature module in
 * `CustomerDestinations.kt`, `AccountDestinations.kt` and `ProviderDestinations.kt`; the ones whose
 * module is not built yet (`feature:booking`, #26) land with their own tickets — wiring a new one in
 * is additive here, not a rewrite.
 */
@Composable
fun App() {
    BooqiTheme {
        val navigator = remember { DefaultNavigator() }
        val backStack by navigator.backStack.collectAsState()
        val destination = backStack.last()

        // Insets are applied once here, for every screen, so no feature draws under the status
        // bar/Dynamic Island, the home indicator, or the keyboard.
        Column(modifier = Modifier.fillMaxSize().safeDrawingPadding()) {
            Box(modifier = Modifier.weight(1f)) {
                DestinationContent(destination = destination, navigator = navigator)
            }
            AppNavigationBar(destination = destination, navigator = navigator)
        }
    }
}

/** Renders the screen for [destination], delegating to the group of its feature module. */
@Composable
private fun DestinationContent(destination: Destination, navigator: Navigator) {
    when (destination) {
        is Destination.Browse,
        is Destination.ServiceDetail,
        is Destination.ProviderProfileView,
        -> CustomerDestination(destination, navigator)
        is Destination.Profile,
        is Destination.Auth,
        is Destination.VerifyEmail,
        -> AccountDestination(destination, navigator)
        is Destination.ProviderProfileSetup,
        is Destination.ServiceList,
        is Destination.ServiceEditor,
        is Destination.ScheduleManagement,
        is Destination.BookingRequestInbox,
        -> ProviderDestination(destination, navigator)
        // Booking, BookingConfirmation, MyBookings, AddressSelection land with feature:booking (#26).
        else -> BrowseScreen(onProviderSelected = { navigator.navigateTo(Destination.Browse) })
    }
}
