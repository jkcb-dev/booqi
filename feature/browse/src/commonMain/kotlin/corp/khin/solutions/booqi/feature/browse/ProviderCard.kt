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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import corp.khin.solutions.booqi.core.designsystem.component.ModalityBadge
import corp.khin.solutions.booqi.core.designsystem.theme.BooqiCornerRadius
import corp.khin.solutions.booqi.core.designsystem.theme.BooqiSpacing
import corp.khin.solutions.booqi.core.designsystem.theme.LocalBooqiExtendedColors
import corp.khin.solutions.booqi.domain.model.ServiceSearchResult

/**
 * One C2 result (the `ProviderCard` organism of docs/design/DESIGN_SYSTEM.md, owned by
 * `feature:browse`): the Service's photo, title, price/duration and modality badge, with its
 * Provider's name, rating and — when the search has a location — the distance. The whole card
 * opens the Service's detail.
 */
@Composable
internal fun ProviderCard(result: ServiceSearchResult, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val service = result.service
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
                Text(result.provider.name, style = MaterialTheme.typography.bodyMedium, color = ink2)
                Row(horizontalArrangement = Arrangement.spacedBy(BooqiSpacing.sm)) {
                    Text(result.provider.ratingSummary(), style = MaterialTheme.typography.bodySmall, color = ink2)
                    result.distanceKm?.let {
                        Text(formatDistanceKm(it), style = MaterialTheme.typography.bodySmall, color = ink2)
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(BooqiSpacing.sm),
                ) {
                    Text(
                        text = "${formatPrice(service.priceCents)} · ${formatDuration(service.durationMinutes)}",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    ModalityBadge(text = service.modality.label())
                }
            }
        }
    }
}
