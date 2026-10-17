@file:OptIn(ExperimentalCoroutinesApi::class)

package corp.khin.solutions.booqi.feature.booking

import corp.khin.solutions.booqi.core.common.DomainError
import corp.khin.solutions.booqi.domain.model.BookingStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.LocalDateTime
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.hours

/**
 * "El Cliente consulta su historial de reservas" / "Un Cliente sin reservas ve su historial vacío"
 * (docs/domain/customer-flow.md § Grupo 3) on C11, through [MyBookingsViewModel]'s reducer over the
 * real [corp.khin.solutions.booqi.domain.usecase.VerHistorialReservasUseCase].
 */
class MyBookingsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val fixture = BookingFixture()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun TestScope.start(viewModel: MyBookingsViewModel) {
        viewModel.onAction(MyBookingsAction.Start)
        advanceUntilIdle()
    }

    private suspend fun TestScope.sendAndAwaitEvent(
        viewModel: MyBookingsViewModel,
        action: MyBookingsAction,
    ): MyBookingsEvent {
        var event: MyBookingsEvent? = null
        val collector = launch { event = viewModel.events.first() }
        viewModel.onAction(action)
        advanceUntilIdle()
        collector.join()
        return checkNotNull(event) { "No event received for $action" }
    }

    private fun seedHistory() {
        fixture.booking("pending", BookingStatus.REQUESTED, LocalDateTime(2026, 10, 14, 10, 0))
        fixture.booking("confirmed-late", BookingStatus.CONFIRMED, LocalDateTime(2026, 10, 20, 10, 0))
        fixture.booking("confirmed-soon", BookingStatus.CONFIRMED, LocalDateTime(2026, 10, 12, 10, 0))
        fixture.booking("done", BookingStatus.COMPLETED, LocalDateTime(2026, 9, 28, 10, 0))
        fixture.booking("cancelled", BookingStatus.CANCELLED_BY_CUSTOMER, LocalDateTime(2026, 9, 30, 10, 0))
        fixture.booking("someone-else", BookingStatus.CONFIRMED, customerId = "other")
    }

    @Test
    fun `the history lists only the customer's own bookings grouped by status`() = runTest(testDispatcher) {
        seedHistory()
        val viewModel = fixture.myBookingsViewModel()

        start(viewModel)

        val state = viewModel.state.value
        assertFalse(state.isLoading)
        assertEquals(
            listOf("pending", "confirmed-soon", "confirmed-late", "done", "cancelled"),
            state.visibleBookings.map { it.id },
        )
        assertEquals(BookingsTab.ALL, state.tab)
        assertEquals("Corte clásico", state.titles["s-local"])
    }

    @Test
    fun `each tab filters by its status`() = runTest(testDispatcher) {
        seedHistory()
        val viewModel = fixture.myBookingsViewModel()
        start(viewModel)

        viewModel.onAction(MyBookingsAction.SelectTab(BookingsTab.CONFIRMED))
        assertEquals(listOf("confirmed-soon", "confirmed-late"), viewModel.state.value.visibleBookings.map { it.id })

        viewModel.onAction(MyBookingsAction.SelectTab(BookingsTab.REQUESTED))
        assertEquals(listOf("pending"), viewModel.state.value.visibleBookings.map { it.id })

        viewModel.onAction(MyBookingsAction.SelectTab(BookingsTab.COMPLETED))
        assertEquals(listOf("done"), viewModel.state.value.visibleBookings.map { it.id })
    }

    @Test
    fun `a pending request past its 24h window is not listed as pending`() = runTest(testDispatcher) {
        fixture.booking("stale", BookingStatus.REQUESTED, requestedAt = BookingFixture.NOW - 25.hours)
        val viewModel = fixture.myBookingsViewModel()
        start(viewModel)

        viewModel.onAction(MyBookingsAction.SelectTab(BookingsTab.REQUESTED))

        assertTrue(viewModel.state.value.visibleBookings.isEmpty())
        assertEquals(1, viewModel.state.value.allBookings.size)
    }

    @Test
    fun `a customer without bookings gets an empty history and not an error`() = runTest(testDispatcher) {
        val viewModel = fixture.myBookingsViewModel()

        start(viewModel)

        val state = viewModel.state.value
        assertTrue(state.hasNoBookings)
        assertNull(state.error)
        assertFalse(state.needsSignIn)
    }

    @Test
    fun `a visitor gets the sign in state`() = runTest(testDispatcher) {
        fixture.users.signedIn = null
        val viewModel = fixture.myBookingsViewModel()

        start(viewModel)

        val state = viewModel.state.value
        assertTrue(state.needsSignIn)
        assertNull(state.error)
        assertFalse(state.hasNoBookings)
        assertEquals(MyBookingsEvent.SignInRequired, sendAndAwaitEvent(viewModel, MyBookingsAction.SignInClicked))
    }

    @Test
    fun `a failing load is reported with retry`() = runTest(testDispatcher) {
        fixture.bookings.listFailure = DomainError.NoConnection
        val viewModel = fixture.myBookingsViewModel()
        start(viewModel)
        assertEquals(DomainError.NoConnection, viewModel.state.value.error)

        fixture.bookings.listFailure = null
        viewModel.onAction(MyBookingsAction.Retry)
        advanceUntilIdle()

        assertNull(viewModel.state.value.error)
    }

    @Test
    fun `tapping a booking opens it and the empty state sends to explore`() = runTest(testDispatcher) {
        seedHistory()
        val viewModel = fixture.myBookingsViewModel()
        start(viewModel)

        assertEquals(
            MyBookingsEvent.BookingSelected("done"),
            sendAndAwaitEvent(viewModel, MyBookingsAction.BookingClicked("done")),
        )
        assertEquals(MyBookingsEvent.Explore, sendAndAwaitEvent(viewModel, MyBookingsAction.ExploreClicked))
    }

    /** Re-entry: the ViewModel is shared, so entering again must reset the tab and reload. */
    @Test
    fun `entering again resets the tab and shows a booking made meanwhile`() = runTest(testDispatcher) {
        seedHistory()
        val viewModel = fixture.myBookingsViewModel()
        start(viewModel)
        viewModel.onAction(MyBookingsAction.SelectTab(BookingsTab.COMPLETED))
        fixture.booking("new", BookingStatus.REQUESTED, LocalDateTime(2026, 10, 13, 10, 0))

        start(viewModel)

        assertEquals(BookingsTab.ALL, viewModel.state.value.tab)
        assertTrue("new" in viewModel.state.value.allBookings.map { it.id })
    }
}
