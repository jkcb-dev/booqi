package corp.khin.solutions.booqi

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import corp.khin.solutions.booqi.core.designsystem.theme.BooqiTheme
import corp.khin.solutions.booqi.core.navigation.DefaultNavigator
import corp.khin.solutions.booqi.core.navigation.Destination
import corp.khin.solutions.booqi.feature.browse.BrowseScreen

/**
 * App root: theme + the single [DefaultNavigator] instance for the whole app. Only one
 * destination (Browse) is wired for real so far — this is intentionally the smallest possible
 * proof that the module graph (domain -> data -> feature:browse, all through Koin, all under one
 * Navigator) works end to end, per the Architect role's definition of done.
 */
@Composable
fun App() {
    BooqiTheme {
        val navigator = remember { DefaultNavigator() }
        val backStack by navigator.backStack.collectAsState()

        when (backStack.last()) {
            is Destination.Browse -> BrowseScreen(
                onProviderSelected = { providerId ->
                    navigator.navigateTo(Destination.ProviderDetail(providerId))
                },
            )
            // Remaining destinations (ProviderDetail, Booking, BookingConfirmation, MyBookings)
            // land with their own feature modules — Navigator/Destination already account for
            // them so wiring a new one is additive here, not a rewrite.
            else -> BrowseScreen(onProviderSelected = { navigator.navigateTo(Destination.Browse) })
        }
    }
}
