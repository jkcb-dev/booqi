@file:OptIn(ExperimentalCoroutinesApi::class)

package corp.khin.solutions.booqi.feature.account

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle

/**
 * Runs [trigger] and suspends until the single event it makes [events] emit is collected. A fresh
 * collector per call means no event is ever left stuck, unread, in the (rendezvous) events Channel
 * for a later call to pick up by accident.
 */
internal suspend fun <T> TestScope.eventAfter(events: Flow<T>, trigger: () -> Unit): T {
    var event: T? = null
    val collector = launch { event = events.first() }
    trigger()
    advanceUntilIdle()
    collector.join()
    return checkNotNull(event) { "No event received" }
}
