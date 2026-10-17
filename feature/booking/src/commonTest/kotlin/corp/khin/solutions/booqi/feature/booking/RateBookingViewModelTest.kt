@file:OptIn(ExperimentalCoroutinesApi::class)

package corp.khin.solutions.booqi.feature.booking

import corp.khin.solutions.booqi.core.common.DomainError
import corp.khin.solutions.booqi.core.common.DomainResult
import corp.khin.solutions.booqi.domain.model.BookingStatus
import corp.khin.solutions.booqi.domain.model.Rating
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
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * "El Cliente califica una cita completada" / "...no completada" (docs/domain/provider-flow.md
 * § Grupo 4) on C12, through [RateBookingViewModel]'s reducer over the real
 * [corp.khin.solutions.booqi.domain.usecase.CalificarCitaUseCase].
 */
class RateBookingViewModelTest {

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

    private fun TestScope.send(viewModel: RateBookingViewModel, vararg actions: RateBookingAction) {
        actions.forEach { viewModel.onAction(it) }
        advanceUntilIdle()
    }

    private suspend fun TestScope.sendAndAwaitEvent(
        viewModel: RateBookingViewModel,
        vararg actions: RateBookingAction,
    ): RateBookingEvent {
        var event: RateBookingEvent? = null
        val collector = launch { event = viewModel.events.first() }
        send(viewModel, *actions)
        collector.join()
        return checkNotNull(event) { "No event received for ${actions.toList()}" }
    }

    @Test
    fun `a completed unrated booking can be rated with stars and a comment`() = runTest(testDispatcher) {
        fixture.booking("b1", BookingStatus.COMPLETED)
        val viewModel = fixture.rateViewModel()
        send(viewModel, RateBookingAction.Start("b1"))
        assertTrue(viewModel.state.value.isRatable)
        assertEquals("Corte clásico", viewModel.state.value.serviceTitle)

        val event = sendAndAwaitEvent(
            viewModel,
            RateBookingAction.StarsSelected(4),
            RateBookingAction.CommentChanged("  Muy buen corte  "),
            RateBookingAction.Submit,
        )

        assertEquals(RateBookingEvent.Finished, event)
        assertEquals(Rating(4, "Muy buen corte"), fixture.bookings.stored("b1").rating)
        val profile = fixture.profiles.getProfile("p1")
        assertEquals(4.0, (profile as DomainResult.Success).value.ratingAverage)
    }

    @Test
    fun `the comment is optional`() = runTest(testDispatcher) {
        fixture.booking("b1", BookingStatus.COMPLETED)
        val viewModel = fixture.rateViewModel()
        send(viewModel, RateBookingAction.Start("b1"), RateBookingAction.StarsSelected(5))

        sendAndAwaitEvent(viewModel, RateBookingAction.Submit)

        assertEquals(Rating(5, null), fixture.bookings.stored("b1").rating)
    }

    @Test
    fun `submitting needs at least one star`() = runTest(testDispatcher) {
        fixture.booking("b1", BookingStatus.COMPLETED)
        val viewModel = fixture.rateViewModel()
        send(viewModel, RateBookingAction.Start("b1"))
        assertFalse(viewModel.state.value.canSubmit)

        send(viewModel, RateBookingAction.Submit)

        assertEquals(0, fixture.bookings.writeCount)
        assertNull(fixture.bookings.stored("b1").rating)
    }

    @Test
    fun `stars outside one to five are ignored`() = runTest(testDispatcher) {
        fixture.booking("b1", BookingStatus.COMPLETED)
        val viewModel = fixture.rateViewModel()
        send(viewModel, RateBookingAction.Start("b1"))

        send(viewModel, RateBookingAction.StarsSelected(6), RateBookingAction.StarsSelected(0))

        assertEquals(0, viewModel.state.value.stars)
    }

    /** "El Cliente intenta calificar una cita no completada". */
    @Test
    fun `a booking that is not completed cannot be rated`() = runTest(testDispatcher) {
        fixture.booking("b1", BookingStatus.CONFIRMED)
        val viewModel = fixture.rateViewModel()
        send(viewModel, RateBookingAction.Start("b1"), RateBookingAction.StarsSelected(5))

        send(viewModel, RateBookingAction.Submit)

        assertFalse(viewModel.state.value.isRatable)
        assertEquals("Solo se puede calificar una cita completada", viewModel.state.value.notRatableReason)
        assertFalse(viewModel.state.value.canSubmit)
        assertEquals(0, fixture.bookings.writeCount)
    }

    @Test
    fun `an already rated booking cannot be rated again`() = runTest(testDispatcher) {
        fixture.bookings.seed(fixture.booking("b1", BookingStatus.COMPLETED).copy(rating = Rating(3, null)))
        val viewModel = fixture.rateViewModel()

        send(viewModel, RateBookingAction.Start("b1"), RateBookingAction.StarsSelected(5), RateBookingAction.Submit)

        assertEquals("Esta cita ya fue calificada", viewModel.state.value.notRatableReason)
        assertEquals(Rating(3, null), fixture.bookings.stored("b1").rating)
    }

    /** A rating saved from elsewhere between load and submit: the domain's message shows inline. */
    @Test
    fun `a rejected submission is reported inline`() = runTest(testDispatcher) {
        fixture.booking("b1", BookingStatus.COMPLETED)
        val viewModel = fixture.rateViewModel()
        send(viewModel, RateBookingAction.Start("b1"), RateBookingAction.StarsSelected(2))
        fixture.bookings.seed(fixture.bookings.stored("b1").copy(rating = Rating(5, null)))

        send(viewModel, RateBookingAction.Submit)

        assertEquals("Esta cita ya fue calificada", viewModel.state.value.formError)
        assertFalse(viewModel.state.value.isSubmitting)
        assertEquals(Rating(5, null), fixture.bookings.stored("b1").rating)
    }

    @Test
    fun `a missing booking is not found`() = runTest(testDispatcher) {
        val viewModel = fixture.rateViewModel()

        send(viewModel, RateBookingAction.Start("nope"))

        assertEquals(DomainError.NotFound, viewModel.state.value.error)
        assertNotNull(viewModel.state.value.bookingId)
    }

    /** Re-entry: rating another booking must not show the previous stars or comment. */
    @Test
    fun `starting another booking resets the form`() = runTest(testDispatcher) {
        fixture.booking("b1", BookingStatus.COMPLETED)
        fixture.booking("b2", BookingStatus.COMPLETED)
        val viewModel = fixture.rateViewModel()
        send(
            viewModel,
            RateBookingAction.Start("b1"),
            RateBookingAction.StarsSelected(3),
            RateBookingAction.CommentChanged("algo"),
        )

        viewModel.onAction(RateBookingAction.Start("b2"))
        assertEquals(0, viewModel.state.value.stars)
        assertEquals("", viewModel.state.value.comment)
        assertTrue(viewModel.state.value.isLoading)

        advanceUntilIdle()
        assertEquals("b2", viewModel.state.value.booking?.id)
    }

    @Test
    fun `back leaves the screen`() = runTest(testDispatcher) {
        val viewModel = fixture.rateViewModel()

        assertEquals(RateBookingEvent.Finished, sendAndAwaitEvent(viewModel, RateBookingAction.BackClicked))
    }
}
