package corp.khin.solutions.booqi.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import corp.khin.solutions.booqi.core.designsystem.theme.BooqiSpacing
import corp.khin.solutions.booqi.core.designsystem.theme.LocalBooqiExtendedColors

/** Every tab the Figma `BottomNavigation` organism can show, with its Spanish label. */
enum class BottomNavigationTab(val label: String) {
    Inicio("Inicio"),
    Buscar("Buscar"),
    Reservas("Reservas"),
    Solicitudes("Solicitudes"),
    Servicios("Servicios"),
    Perfil("Perfil"),
}

/**
 * The two confirmed variants of the organism (docs/design/DESIGN_SYSTEM.md): the Cliente session
 * shell and the Proveedor one. [tabs] is the full Figma tab list of each, in order.
 */
enum class BottomNavigationVariant(val tabs: List<BottomNavigationTab>) {
    Cliente(
        listOf(
            BottomNavigationTab.Inicio,
            BottomNavigationTab.Buscar,
            BottomNavigationTab.Reservas,
            BottomNavigationTab.Perfil,
        ),
    ),
    Proveedor(
        listOf(
            BottomNavigationTab.Inicio,
            BottomNavigationTab.Solicitudes,
            BottomNavigationTab.Servicios,
            BottomNavigationTab.Perfil,
        ),
    ),
}

/**
 * Bottom navigation organism (docs/design/DESIGN_SYSTEM.md). Lives in `core:designsystem` because
 * both session shells (Cliente, Proveedor) need it. It only knows tabs, never destinations: the
 * caller maps [onSelect] to navigation.
 *
 * [availableTabs] narrows [variant] to the tabs whose screen exists, so a tab is never shown that
 * would lead nowhere; they keep the variant's Figma order. There is no icon yet (the icon set is
 * not chosen, same as [EmptyState]), so the selected tab is marked by weight and color.
 */
@Composable
fun BottomNavigation(
    variant: BottomNavigationVariant,
    selected: BottomNavigationTab,
    availableTabs: Set<BottomNavigationTab>,
    onSelect: (BottomNavigationTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surface) {
        Column {
            HorizontalDivider(color = LocalBooqiExtendedColors.current.border)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                variant.tabs.filter { it in availableTabs }.forEach { tab ->
                    BottomNavigationItem(
                        tab = tab,
                        isSelected = tab == selected,
                        onClick = { onSelect(tab) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun BottomNavigationItem(
    tab: BottomNavigationTab,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .selectable(selected = isSelected, role = Role.Tab, onClick = onClick)
            .padding(vertical = BooqiSpacing.sm),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = tab.label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) MaterialTheme.colorScheme.primary else LocalBooqiExtendedColors.current.ink2,
        )
    }
}
