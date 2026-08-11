package corp.khin.solutions.booqi.core.navigation

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Minimal stack-based [Navigator]. Deliberately has no dependency on a Compose navigation
 * library — with a single feature (`browse`) wired so far there's nothing to gain from adopting
 * one yet. Swap the implementation for a real nav-library-backed one once there are enough
 * screens (booking flow) that deep-linking/animation/back-stack-restoration start to matter; the
 * [Navigator] interface is what the rest of the app depends on, not this class.
 */
class DefaultNavigator(startDestination: Destination = Destination.Browse) : Navigator {

    private val stack = MutableStateFlow(listOf(startDestination))
    override val backStack: StateFlow<List<Destination>> = stack.asStateFlow()
    override val current: Destination get() = stack.value.last()

    override fun navigateTo(destination: Destination) {
        stack.value = stack.value + destination
    }

    override fun navigateBack(): Boolean {
        if (stack.value.size <= 1) return false
        stack.value = stack.value.dropLast(1)
        return true
    }
}
