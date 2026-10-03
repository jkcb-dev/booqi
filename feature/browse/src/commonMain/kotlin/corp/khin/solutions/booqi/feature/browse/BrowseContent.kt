package corp.khin.solutions.booqi.feature.browse

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import corp.khin.solutions.booqi.core.designsystem.theme.BooqiSpacing

/**
 * The search screen: the "Buscar servicios" heading, the text field and the category chips (always
 * visible, in every state) over a body that is either C1's "Recientes" (before the first search)
 * or C2's results.
 */
@Composable
internal fun BrowseContent(
    state: BrowseUiState,
    onAction: (BrowseAction) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(BooqiSpacing.sm),
    ) {
        Text(
            text = "Buscar servicios",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(start = BooqiSpacing.md, top = BooqiSpacing.md, end = BooqiSpacing.md),
        )
        SearchField(query = state.query, onAction = onAction)
        CategoryChips(selected = state.category, onAction = onAction)
        Box(modifier = Modifier.weight(1f)) {
            if (state.hasSearched) {
                ResultsContent(state = state, onAction = onAction)
            } else {
                RecentSearches(recents = state.recentSearches, onAction = onAction)
            }
        }
    }
}
