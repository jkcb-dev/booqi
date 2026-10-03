package corp.khin.solutions.booqi.feature.browse

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
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
import corp.khin.solutions.booqi.domain.model.ProviderSummary
import corp.khin.solutions.booqi.domain.model.ServiceDetail

/** C3's loaded body: title, price, duration, modality, description, the Provider block (-> C4)
 * and "Reservar". Scrolls as a whole so a long description never pushes the button off screen. */
@Composable
internal fun ServiceDetailBody(detail: ServiceDetail, onAction: (ServiceDetailAction) -> Unit) {
    val service = detail.service
    val ink2 = LocalBooqiExtendedColors.current.ink2
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(BooqiSpacing.md),
        verticalArrangement = Arrangement.spacedBy(BooqiSpacing.md),
    ) {
        PhotoPlaceholder(
            name = service.title,
            modifier = Modifier.fillMaxWidth().height(BooqiSpacing.xxxl + BooqiSpacing.xxxl),
        )
        Text(service.title, style = MaterialTheme.typography.headlineSmall)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(BooqiSpacing.sm),
        ) {
            Text(formatPrice(service.priceCents), style = MaterialTheme.typography.titleMedium)
            Text(formatDuration(service.durationMinutes), style = MaterialTheme.typography.bodyMedium, color = ink2)
            ModalityBadge(text = service.modality.label())
        }
        Text("Descripción", style = MaterialTheme.typography.titleMedium)
        Text(service.description, style = MaterialTheme.typography.bodyMedium)
        Text("Proveedor", style = MaterialTheme.typography.titleMedium)
        ProviderBlock(provider = detail.provider, onClick = { onAction(ServiceDetailAction.ProviderClicked) })
        Button(onClick = { onAction(ServiceDetailAction.BookClicked) }, modifier = Modifier.fillMaxWidth()) {
            Text("Reservar")
        }
    }
}

/** The tappable Provider block of C3: photo, name, rating and "Ver perfil ›". */
@Composable
private fun ProviderBlock(provider: ProviderSummary, onClick: () -> Unit) {
    val ink2 = LocalBooqiExtendedColors.current.ink2
    OutlinedCard(
        onClick = onClick,
        shape = RoundedCornerShape(BooqiCornerRadius.medium),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(BooqiSpacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(BooqiSpacing.sm),
        ) {
            PhotoPlaceholder(name = provider.name, modifier = Modifier.size(BooqiSpacing.xxl))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(BooqiSpacing.xxs)) {
                Text(provider.name, style = MaterialTheme.typography.titleMedium)
                Text(provider.ratingSummary(), style = MaterialTheme.typography.bodySmall, color = ink2)
            }
            Text("Ver perfil ›", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        }
    }
}
