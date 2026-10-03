package corp.khin.solutions.booqi.core.designsystem.component

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import corp.khin.solutions.booqi.core.designsystem.theme.BooqiCornerRadius

/**
 * Category chip atom (docs/design/DESIGN_SYSTEM.md — Todos/Barber/Uñas/Limpieza/Masajes/Técnico,
 * Figma C1). Lives in `core:designsystem` as an atom, per the module-ownership table. Takes the
 * already-localized [label] rather than a domain `ServiceCategory` so the design system never
 * depends on `domain`; each feature maps its own category to the label.
 *
 * It is a plain single-select filter chip, so `feature:browse` also uses it for the C2 distance
 * filter (1/2/5/10 km) instead of re-skinning a second chip.
 */
@Composable
fun CategoryChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, style = MaterialTheme.typography.labelLarge) },
        shape = RoundedCornerShape(BooqiCornerRadius.pill),
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
        modifier = modifier,
    )
}
