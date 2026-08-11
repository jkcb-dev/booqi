package corp.khin.solutions.booqi.core.navigation

import kotlinx.coroutines.flow.StateFlow

/**
 * The only channel through which one feature reaches another. A ViewModel takes a [Navigator] via
 * DI and calls it in response to a [corp.khin.solutions.booqi.core.navigation.Destination]-shaped
 * `Event` — it never imports another `feature:*` module's screen or ViewModel directly.
 */
interface Navigator {
    val backStack: StateFlow<List<Destination>>
    val current: Destination

    fun navigateTo(destination: Destination)
    fun navigateBack(): Boolean
}
