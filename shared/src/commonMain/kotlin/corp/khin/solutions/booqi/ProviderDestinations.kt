package corp.khin.solutions.booqi

import androidx.compose.runtime.Composable
import corp.khin.solutions.booqi.core.navigation.Destination
import corp.khin.solutions.booqi.core.navigation.Navigator
import corp.khin.solutions.booqi.feature.provider.BookingRequestInboxScreen
import corp.khin.solutions.booqi.feature.provider.ProviderProfileNavigation
import corp.khin.solutions.booqi.feature.provider.ProviderProfileScreen
import corp.khin.solutions.booqi.feature.provider.ScheduleManagementScreen
import corp.khin.solutions.booqi.feature.provider.ServiceEditorScreen
import corp.khin.solutions.booqi.feature.provider.ServiceListScreen

/**
 * `feature:provider` — the Provider area, reached from the profile's "Convertirme en Proveedor" (which
 * runs the access guard first). Its screens act for the signed-in User's ProviderProfile. A "Volver"
 * from a screen that is the only back-stack entry (reached through a tab) falls back to the
 * Provider's profile, the shell's home.
 */
@Composable
internal fun ProviderDestination(destination: Destination, navigator: Navigator) {
    when (destination) {
        is Destination.ProviderProfileSetup -> ProviderProfileScreen(
            navigation = ProviderProfileNavigation(
                onManageServices = { navigator.navigateTo(Destination.ServiceList) },
                onManageSchedule = { navigator.navigateTo(Destination.ScheduleManagement) },
                onManageBookings = { navigator.navigateTo(Destination.BookingRequestInbox) },
                onSwitchToCustomer = { navigator.navigateToRoot(Destination.Profile) },
            ),
        )
        is Destination.ServiceList -> ServiceListScreen(
            onAddService = { navigator.navigateTo(Destination.ServiceEditor()) },
            onEditService = { id -> navigator.navigateTo(Destination.ServiceEditor(id)) },
        )
        is Destination.BookingRequestInbox -> BookingRequestInboxScreen(
            onFinished = { navigator.backOr(Destination.ProviderProfileSetup) },
        )
        is Destination.ScheduleManagement -> ScheduleManagementScreen(
            onFinished = { navigator.backOr(Destination.ProviderProfileSetup) },
        )
        is Destination.ServiceEditor -> ServiceEditorScreen(
            serviceId = destination.serviceId,
            onFinished = { navigator.backOr(Destination.ServiceList) },
        )
        else -> Unit
    }
}
