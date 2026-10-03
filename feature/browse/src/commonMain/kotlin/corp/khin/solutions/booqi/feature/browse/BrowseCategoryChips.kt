package corp.khin.solutions.booqi.feature.browse

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import corp.khin.solutions.booqi.core.designsystem.component.CategoryChip
import corp.khin.solutions.booqi.core.designsystem.theme.BooqiSpacing
import corp.khin.solutions.booqi.domain.model.ServiceCategory

/**
 * The C1 chips under the text field: "Todos" (no category) and then `ServiceCategory.filterable`,
 * in that order. Tapping one searches straight away.
 */
@Composable
internal fun CategoryChips(selected: ServiceCategory?, onAction: (BrowseAction) -> Unit) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = BooqiSpacing.md),
        horizontalArrangement = Arrangement.spacedBy(BooqiSpacing.xs),
    ) {
        CategoryChip(
            label = "Todos",
            selected = selected == null,
            onClick = { onAction(BrowseAction.CategorySelected(null)) },
        )
        ServiceCategory.filterable.forEach { category ->
            CategoryChip(
                label = category.label(),
                selected = selected == category,
                onClick = { onAction(BrowseAction.CategorySelected(category)) },
            )
        }
    }
}
