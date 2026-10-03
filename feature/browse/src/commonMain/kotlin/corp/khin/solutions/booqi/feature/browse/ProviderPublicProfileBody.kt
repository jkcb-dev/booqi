package corp.khin.solutions.booqi.feature.browse

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import corp.khin.solutions.booqi.core.designsystem.component.RatingDisplay
import corp.khin.solutions.booqi.core.designsystem.theme.BooqiSpacing
import corp.khin.solutions.booqi.core.designsystem.theme.LocalBooqiExtendedColors
import corp.khin.solutions.booqi.domain.model.ProviderPublicProfile
import corp.khin.solutions.booqi.domain.model.ProviderSummary
import kotlinx.datetime.TimeZone

/**
 * C4's loaded body: bio and location, the "Servicios" section (one card per active Service, each
 * with "Reservar ›") and the shared `RatingDisplay` (average, histogram, individual reviews).
 * One lazy list, so a Provider with many Services still scrolls smoothly.
 */
@Composable
internal fun ProviderPublicProfileBody(
    profile: ProviderPublicProfile,
    onAction: (ProviderPublicProfileAction) -> Unit,
) {
    val timeZone = TimeZone.currentSystemDefault()
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(BooqiSpacing.md),
        verticalArrangement = Arrangement.spacedBy(BooqiSpacing.md),
    ) {
        item { ProviderIntro(profile.provider) }
        item { Text("Servicios", style = MaterialTheme.typography.titleMedium) }
        if (profile.services.isEmpty()) {
            item {
                Text(
                    text = "Este proveedor todavía no tiene servicios disponibles.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = LocalBooqiExtendedColors.current.ink2,
                )
            }
        }
        items(profile.services, key = { it.id }) { service ->
            ProviderServiceCard(
                service = service,
                onClick = { onAction(ProviderPublicProfileAction.ServiceClicked(service.id)) },
                onBook = { onAction(ProviderPublicProfileAction.BookClicked(service.id)) },
            )
        }
        item { Text("Calificaciones", style = MaterialTheme.typography.titleMedium) }
        item {
            RatingDisplay(
                average = profile.provider.ratingAverage,
                count = profile.provider.ratingCount,
                reviews = profile.reviews.map { it.toRatingReview(timeZone) },
            )
        }
    }
}

/** Photo, name, location and bio. */
@Composable
private fun ProviderIntro(provider: ProviderSummary) {
    val ink2 = LocalBooqiExtendedColors.current.ink2
    Column(verticalArrangement = Arrangement.spacedBy(BooqiSpacing.md)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(BooqiSpacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PhotoPlaceholder(name = provider.name, modifier = Modifier.size(BooqiSpacing.xxxl))
            Column(verticalArrangement = Arrangement.spacedBy(BooqiSpacing.xxs)) {
                Text(provider.name, style = MaterialTheme.typography.headlineSmall)
                provider.location?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = ink2) }
            }
        }
        provider.description?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
    }
}
