package corp.khin.solutions.booqi.feature.provider

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import corp.khin.solutions.booqi.core.designsystem.component.ModalityBadge
import corp.khin.solutions.booqi.core.designsystem.theme.BooqiCornerRadius
import corp.khin.solutions.booqi.core.designsystem.theme.BooqiSpacing
import corp.khin.solutions.booqi.core.designsystem.theme.LocalBooqiExtendedColors
import corp.khin.solutions.booqi.domain.model.Service

/** One P4 row: enable/disable toggle, title, price, duration, modality badge and the Editar
 * button. A disabled service is still listed, just visually muted ("Deshabilitado"). */
@Composable
internal fun ServiceListItem(
    service: Service,
    isToggling: Boolean,
    onAction: (ServiceListAction) -> Unit,
) {
    val ink2 = LocalBooqiExtendedColors.current.ink2
    Card(
        shape = RoundedCornerShape(BooqiCornerRadius.medium),
        colors = CardDefaults.cardColors(
            containerColor = if (service.isActive) {
                MaterialTheme.colorScheme.surface
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(BooqiSpacing.md),
            verticalArrangement = Arrangement.spacedBy(BooqiSpacing.xs),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = service.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (service.isActive) MaterialTheme.colorScheme.onSurface else ink2,
                    modifier = Modifier.weight(1f),
                )
                Switch(
                    checked = service.isActive,
                    onCheckedChange = { onAction(ServiceListAction.SetServiceEnabled(service.id, it)) },
                    enabled = !isToggling,
                )
            }
            Text(
                text = "\$${formatPriceInput(service.priceCents)} · ${service.durationMinutes} min",
                style = MaterialTheme.typography.bodyMedium,
                color = ink2,
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(BooqiSpacing.sm),
            ) {
                ModalityBadge(text = service.modality.label())
                if (!service.isActive) {
                    Text("Deshabilitado", style = MaterialTheme.typography.labelSmall, color = ink2)
                }
            }
            OutlinedButton(
                onClick = { onAction(ServiceListAction.EditServiceClicked(service.id)) },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Editar") }
        }
    }
}
