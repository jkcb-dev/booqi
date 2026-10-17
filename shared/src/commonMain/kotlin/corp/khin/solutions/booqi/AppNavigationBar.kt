package corp.khin.solutions.booqi

import androidx.compose.runtime.Composable
import corp.khin.solutions.booqi.core.designsystem.component.BottomNavigation
import corp.khin.solutions.booqi.core.designsystem.component.BottomNavigationTab
import corp.khin.solutions.booqi.core.designsystem.component.BottomNavigationVariant
import corp.khin.solutions.booqi.core.navigation.Destination
import corp.khin.solutions.booqi.core.navigation.Navigator

/**
 * Which tabs of the Figma `BottomNavigation` exist today. Only tabs whose screen is built are
 * shown, so no tab leads nowhere:
 * - **Cliente:** Buscar and Perfil. Omitted: *Inicio* (no home screen is designed or built yet) and
 *   *Reservas* (the Customer's booking history lands with `feature:booking`, #26).
 * - **Proveedor:** Solicitudes, Servicios and Perfil. Omitted: *Inicio* (same reason). The Provider
 *   area is reached from the Cliente "Perfil" tab ("Convertirme en Proveedor" / "Ir a mi panel"), and
 *   left through its own screen's "Volver al modo Cliente".
 */
private val AVAILABLE_TABS: Map<BottomNavigationVariant, Set<BottomNavigationTab>> = mapOf(
    BottomNavigationVariant.Cliente to setOf(BottomNavigationTab.Buscar, BottomNavigationTab.Perfil),
    BottomNavigationVariant.Proveedor to setOf(
        BottomNavigationTab.Solicitudes,
        BottomNavigationTab.Servicios,
        BottomNavigationTab.Perfil,
    ),
)

/** The shell a destination lives in and the tab it highlights. */
private class ShellPosition(val variant: BottomNavigationVariant, val selected: BottomNavigationTab)

/** `null` for the screens that show no bar: the sign-in flow, the verification notice, the service
 * editor (a form to finish or abandon, not a tab) and destinations that are not wired yet. */
private fun Destination.shellPosition(): ShellPosition? = when (this) {
    is Destination.Browse,
    is Destination.ServiceDetail,
    is Destination.ProviderProfileView,
    -> ShellPosition(BottomNavigationVariant.Cliente, BottomNavigationTab.Buscar)
    is Destination.Profile -> ShellPosition(BottomNavigationVariant.Cliente, BottomNavigationTab.Perfil)
    is Destination.BookingRequestInbox ->
        ShellPosition(BottomNavigationVariant.Proveedor, BottomNavigationTab.Solicitudes)
    is Destination.ServiceList -> ShellPosition(BottomNavigationVariant.Proveedor, BottomNavigationTab.Servicios)
    is Destination.ProviderProfileSetup,
    is Destination.ScheduleManagement,
    -> ShellPosition(BottomNavigationVariant.Proveedor, BottomNavigationTab.Perfil)
    else -> null
}

/** The top-level destination a tab leads to. */
private fun rootOf(variant: BottomNavigationVariant, tab: BottomNavigationTab): Destination? = when (variant) {
    BottomNavigationVariant.Cliente -> when (tab) {
        BottomNavigationTab.Buscar -> Destination.Browse
        BottomNavigationTab.Perfil -> Destination.Profile
        else -> null
    }
    BottomNavigationVariant.Proveedor -> when (tab) {
        BottomNavigationTab.Solicitudes -> Destination.BookingRequestInbox
        BottomNavigationTab.Servicios -> Destination.ServiceList
        BottomNavigationTab.Perfil -> Destination.ProviderProfileSetup
        else -> null
    }
}

/** The bottom navigation for [destination]'s shell, or nothing where a screen shows none. Switching
 * tabs replaces the back stack ([Navigator.navigateToRoot]) instead of stacking screens. */
@Composable
internal fun AppNavigationBar(destination: Destination, navigator: Navigator) {
    val position = destination.shellPosition() ?: return
    BottomNavigation(
        variant = position.variant,
        selected = position.selected,
        availableTabs = AVAILABLE_TABS.getValue(position.variant),
        onSelect = { tab -> rootOf(position.variant, tab)?.let(navigator::navigateToRoot) },
    )
}

/** Back, or — when this screen is already the only entry (reached through a tab) — [fallback]. */
internal fun Navigator.backOr(fallback: Destination) {
    if (!navigateBack()) navigateToRoot(fallback)
}
