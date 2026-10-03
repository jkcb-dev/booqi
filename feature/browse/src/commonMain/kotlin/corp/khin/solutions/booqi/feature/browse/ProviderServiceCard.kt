package corp.khin.solutions.booqi.feature.browse

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import corp.khin.solutions.booqi.core.designsystem.component.ModalityBadge
import corp.khin.solutions.booqi.core.designsystem.theme.BooqiCornerRadius
import corp.khin.solutions.booqi.core.designsystem.theme.BooqiSpacing
import corp.khin.solutions.booqi.core.designsystem.theme.LocalBooqiExtendedColors
import corp.khin.solutions.booqi.domain.model.Service

/**
 * One card of C4's "Servicios" section (docs/design/SCREENS.md: photo, title, duration + modality,
 * price and "Reservar ›"). The card opens the Service's detail ([onClick]); "Reservar ›" is the
 * shortcut straight to booking ([onBook]).
 */
@Composable
internal fun ProviderServiceCard(
    service: Service,
    onClick: () -> Unit,
    onBook: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val ink2 = LocalBooqiExtendedColors.current.ink2
    OutlinedCard(
        onClick = onClick,
        shape = RoundedCornerShape(BooqiCornerRadius.medium),
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(BooqiSpacing.md),
            horizontalArrangement = Arrangement.spacedBy(BooqiSpacing.sm),
        ) {
            PhotoPlaceholder(name = service.title, modifier = Modifier.size(BooqiSpacing.xxxl))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(BooqiSpacing.xxs)) {
                Text(service.title, style = MaterialTheme.typography.titleMedium)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(BooqiSpacing.sm),
                ) {
                    Text(
                        text = formatDuration(service.durationMinutes),
                        style = MaterialTheme.typography.bodySmall,
                        color = ink2,
                    )
                    ModalityBadge(text = service.modality.label())
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = formatPrice(service.priceCents),
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = onBook) { Text("Reservar ›") }
                }
            }
        }
    }
}
