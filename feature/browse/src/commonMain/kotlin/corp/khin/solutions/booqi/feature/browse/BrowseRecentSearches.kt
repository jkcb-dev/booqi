package corp.khin.solutions.booqi.feature.browse

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import corp.khin.solutions.booqi.core.designsystem.theme.BooqiSpacing
import corp.khin.solutions.booqi.core.designsystem.theme.LocalBooqiExtendedColors

/**
 * C1's "Recientes": the texts searched this session (see [BrowseViewModel] for why session only),
 * newest first. Tapping one searches for it again.
 */
@Composable
internal fun RecentSearches(recents: List<String>, onAction: (BrowseAction) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = BooqiSpacing.md),
    ) {
        item {
            Text(
                text = "Recientes",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(vertical = BooqiSpacing.xs),
            )
        }
        if (recents.isEmpty()) {
            item {
                Text(
                    text = "Todavía no buscaste nada. Escribí arriba o elegí una categoría.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = LocalBooqiExtendedColors.current.ink2,
                )
            }
        }
        items(recents, key = { it }) { recent ->
            Text(
                text = recent,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onAction(BrowseAction.RecentSelected(recent)) }
                    .padding(vertical = BooqiSpacing.sm),
            )
        }
    }
}
