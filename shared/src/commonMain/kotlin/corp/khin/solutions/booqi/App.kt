package corp.khin.solutions.booqi

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import corp.khin.solutions.booqi.core.designsystem.theme.BooqiSpacing
import corp.khin.solutions.booqi.core.designsystem.theme.BooqiTheme
import corp.khin.solutions.booqi.core.navigation.DefaultNavigator
import corp.khin.solutions.booqi.core.navigation.Destination
import corp.khin.solutions.booqi.feature.browse.BrowseScreen
import corp.khin.solutions.booqi.feature.provider.ProviderProfileScreen

/**
 * App root: theme + the single [DefaultNavigator] instance for the whole app.
 * [Destination.Browse] (`feature:browse`) and [Destination.ProviderProfileSetup]
 * (`feature:provider`) are wired for real — the rest land with their own feature modules as those
 * tickets ship. Navigator/Destination already account for all of them so wiring a new one in is
 * additive here, not a rewrite.
 */
@Composable
fun App() {
    BooqiTheme {
        val navigator = remember { DefaultNavigator() }
        val backStack by navigator.backStack.collectAsState()

        Box(modifier = Modifier.fillMaxSize()) {
            when (backStack.last()) {
                is Destination.Browse -> BrowseScreen(
                    onProviderSelected = { providerId ->
                        navigator.navigateTo(Destination.ProviderDetail(providerId))
                    },
                )
                is Destination.ProviderProfileSetup -> ProviderProfileScreen()
                // Remaining destinations land with their own feature modules as those tickets
                // ship: ProviderDetail, ProviderProfileView (feature:browse); Booking,
                // BookingConfirmation, MyBookings, AddressSelection (feature:booking);
                // ServiceList, ServiceEditor, ScheduleManagement, BookingRequestInbox
                // (feature:provider).
                else -> BrowseScreen(onProviderSelected = { navigator.navigateTo(Destination.Browse) })
            }

            // TEMPORARY: there is no "become a Provider" entry point in the product yet (that's
            // normally reached from a User's own profile/settings, which doesn't exist — Identity
            // bounded context isn't built, see docs/DOMAIN.md). This button switches between
            // Browse and the Provider flow so both are reachable for manual verification ahead of
            // that real entry point. Remove once a real navigation trigger exists.
            val onProviderScreen = backStack.last() is Destination.ProviderProfileSetup
            FilledTonalButton(
                onClick = {
                    navigator.navigateTo(
                        if (onProviderScreen) Destination.Browse else Destination.ProviderProfileSetup,
                    )
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(BooqiSpacing.md),
            ) {
                Text(if (onProviderScreen) "Volver a Browse" else "Modo Proveedor")
            }
        }
    }
}
