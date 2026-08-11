package corp.khin.solutions.booqi.feature.browse

import corp.khin.solutions.booqi.domain.model.ServiceProvider

/** User intents. The Composable only ever calls [BrowseViewModel.onAction] with one of these. */
sealed interface BrowseAction {
    data object Refresh : BrowseAction
    data class SelectProvider(val provider: ServiceProvider) : BrowseAction
}
