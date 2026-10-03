package corp.khin.solutions.booqi.feature.provider

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import corp.khin.solutions.booqi.core.designsystem.theme.BooqiSpacing
import corp.khin.solutions.booqi.domain.model.ServiceModality

@OptIn(ExperimentalLayoutApi::class)
/** Single-choice modalidad selector (Local / Domicilio / Local & Dom.) for the P5 form. */
@Composable
internal fun ServiceModalityPicker(
    selected: ServiceModality,
    onSelected: (ServiceModality) -> Unit,
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(BooqiSpacing.xs),
    ) {
        ServiceModality.entries.forEach { modality ->
            FilterChip(
                selected = modality == selected,
                onClick = { onSelected(modality) },
                label = { Text(modality.label()) },
            )
        }
    }
}
