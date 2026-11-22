package corp.khin.solutions.booqi.feature.provider

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import corp.khin.solutions.booqi.core.designsystem.component.CategoryChip
import corp.khin.solutions.booqi.core.designsystem.theme.BooqiSpacing
import corp.khin.solutions.booqi.domain.model.ServiceCategory

/**
 * Single-choice category selector for the P5 form, built from the [CategoryChip] atom. Offers
 * [ServiceCategory.filterable] only: OTRO has no chip on C1, so picking it would hide the service
 * from every category search — the Provider must choose a real one. [selected] `null` = none yet.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ServiceCategoryPicker(
    selected: ServiceCategory?,
    onSelected: (ServiceCategory) -> Unit,
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(BooqiSpacing.xs),
    ) {
        ServiceCategory.filterable.forEach { category ->
            CategoryChip(
                label = category.label(),
                selected = category == selected,
                onClick = { onSelected(category) },
            )
        }
    }
}
