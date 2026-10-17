@file:OptIn(ExperimentalCoroutinesApi::class)

package corp.khin.solutions.booqi.feature.booking

import corp.khin.solutions.booqi.core.common.DomainError
import corp.khin.solutions.booqi.domain.model.Booking
import corp.khin.solutions.booqi.domain.model.BookingStatus
import corp.khin.solutions.booqi.domain.model.CustomerReasonCode
import corp.khin.solutions.booqi.domain.model.ProviderReasonCode
import corp.khin.solutions.booqi.domain.model.Rating
import corp.khin.solutions.booqi.domain.model.Reason
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
import kotlin.time.Duration.Companion.minutes

/**
 * Customer flow Grupo 3 on C8-C10b (pending / confirmed / rejected / expired), the cancel action and
 * its 3-hour window, through [BookingDetailViewModel]'s reducer over the real use cases. Time is
 * Monday 5 October 2026 11:00 UTC (the tests use UTC).
 */
class BookingDetailViewModelTest {

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

    private fun TestScope.start(viewModel: BookingDetailViewModel, bookingId: String) {
        viewModel.onAction(BookingDetailAction.Start(bookingId))
        advanceUntilIdle()
    }

    private fun TestScope.send(viewModel: BookingDetailViewModel, vararg actions: BookingDetailAction) {
        actions.forEach { viewModel.onAction(it) }
        advanceUntilIdle()
    }

    private suspend fun TestScope.sendAndAwaitEvent(
        viewModel: BookingDetailViewModel,
        action: BookingDetailAction,
    ): BookingDetailEvent {
        var event: BookingDetailEvent? = null
        val collector = launch { event = viewModel.events.first() }
        send(viewModel, action)
        collector.join()
        return checkNotNull(event) { "No event received for $action" }
    }

    private fun confirmedAt(hour: Int, minute: Int = 0, day: Int = 5, second: Int = 0): Booking =
        fixture.booking("b1", BookingStatus.CONFIRMED, LocalDateTime(2026, 10, day, hour, minute, second))

    // --- C8 pending ----------------------------------------------------------------------------

    @Test
    fun `a pending request shows the 24h countdown`() = runTest(testDispatcher) {
        fixture.booking("b1", BookingStatus.REQUESTED, requestedAt = BookingFixture.NOW - 1.hours)
        val viewModel = fixture.detailViewModel()

        start(viewModel, "b1")

        val state = viewModel.state.value
        assertEquals(BookingDetailVariant.PENDING, state.variant)
        assertEquals("23 h 0 min", state.countdown)
        assertEquals("Corte clásico", state.serviceTitle)
        assertEquals("Studio Booqi", state.providerName)
        assertFalse(state.canCancel)
    }

    @Test
    fun `the countdown follows the clock`() = runTest(testDispatcher) {
        fixture.booking("b1", BookingStatus.REQUESTED, requestedAt = BookingFixture.NOW - 1.hours)
        val viewModel = fixture.detailViewModel()
        start(viewModel, "b1")

        fixture.clock.instant = BookingFixture.NOW + 22.hours + 30.minutes
        send(viewModel, BookingDetailAction.Tick)

        assertEquals("30 min", viewModel.state.value.countdown)
    }

    /** The 24h window lapsed but the sweep has not stored EXPIRED yet: shown as expired, not "Pendiente". */
    @Test
    fun `a pending request past its 24h window reads as expired`() = runTest(testDispatcher) {
        fixture.booking("b1", BookingStatus.REQUESTED, requestedAt = BookingFixture.NOW - 25.hours)
        val viewModel = fixture.detailViewModel()

        start(viewModel, "b1")

        assertEquals(BookingDetailVariant.EXPIRED, viewModel.state.value.variant)
        assertNull(viewModel.state.value.countdown)
    }

    // --- C9 confirmed + cancel -----------------------------------------------------------------

    @Test
    fun `a confirmed booking shows the countdown to the appointment`() = runTest(testDispatcher) {
        fixture.booking("b1", BookingStatus.CONFIRMED, LocalDateTime(2026, 10, 12, 10, 0))
        val viewModel = fixture.detailViewModel()

        start(viewModel, "b1")

        assertEquals(BookingDetailVariant.CONFIRMED, viewModel.state.value.variant)
        assertEquals("6 d 23 h", viewModel.state.value.countdown)
        assertTrue(viewModel.state.value.canCancel)
    }

    /** "El Cliente cancela una reserva confirmada a tiempo". */
    @Test
    fun `cancelling in time with a reason moves the booking to cancelled by customer`() = runTest(testDispatcher) {
        confirmedAt(hour = 18)
        val viewModel = fixture.detailViewModel()
        start(viewModel, "b1")

        send(
            viewModel,
            BookingDetailAction.OpenCancelForm,
            BookingDetailAction.ReasonSelected(CustomerReasonCode.CHANGE_OF_PLANS),
            BookingDetailAction.ReasonNoteChanged("texto que no debe viajar"),
            BookingDetailAction.ConfirmCancel,
        )

        val stored = fixture.bookings.stored("b1")
        assertEquals(BookingStatus.CANCELLED_BY_CUSTOMER, stored.status)
        assertEquals(CustomerReasonCode.CHANGE_OF_PLANS, stored.reason?.code)
        assertNull(stored.reason?.note)
        val state = viewModel.state.value
        assertEquals(BookingDetailVariant.CANCELLED, state.variant)
        assertNull(state.cancelForm)
        assertFalse(state.isCancelling)
        assertEquals("Cita cancelada", state.notice)
    }

    @Test
    fun `the free text travels only with the Otro reason`() = runTest(testDispatcher) {
        confirmedAt(hour = 18)
        val viewModel = fixture.detailViewModel()
        start(viewModel, "b1")

        send(
            viewModel,
            BookingDetailAction.OpenCancelForm,
            BookingDetailAction.ReasonSelected(CustomerReasonCode.OTHER),
            BookingDetailAction.ReasonNoteChanged("Surgió un viaje"),
            BookingDetailAction.ConfirmCancel,
        )

        assertEquals(Reason(CustomerReasonCode.OTHER, "Surgió un viaje"), fixture.bookings.stored("b1").reason)
    }

    @Test
    fun `cancelling needs a reason`() = runTest(testDispatcher) {
        confirmedAt(hour = 18)
        val viewModel = fixture.detailViewModel()
        start(viewModel, "b1")

        send(viewModel, BookingDetailAction.OpenCancelForm, BookingDetailAction.ConfirmCancel)

        assertEquals("Elige un motivo para continuar", viewModel.state.value.cancelForm?.error)
        assertEquals(0, fixture.bookings.writeCount)
        assertEquals(BookingStatus.CONFIRMED, fixture.bookings.stored("b1").status)
    }

    @Test
    fun `closing the picker keeps the appointment`() = runTest(testDispatcher) {
        confirmedAt(hour = 18)
        val viewModel = fixture.detailViewModel()
        start(viewModel, "b1")

        send(viewModel, BookingDetailAction.OpenCancelForm, BookingDetailAction.CloseCancelForm)

        assertNull(viewModel.state.value.cancelForm)
        assertEquals(BookingStatus.CONFIRMED, fixture.bookings.stored("b1").status)
    }

    /** "El Cliente intenta cancelar fuera del plazo permitido": no picker, a message, nothing stored. */
    @Test
    fun `inside the 3 hour window cancelling is not offered`() = runTest(testDispatcher) {
        confirmedAt(hour = 13, minute = 59, second = 59)
        val viewModel = fixture.detailViewModel()
        start(viewModel, "b1")
        assertFalse(viewModel.state.value.canCancel)

        send(viewModel, BookingDetailAction.OpenCancelForm)

        assertNull(viewModel.state.value.cancelForm)
        assertEquals(Booking.TOO_LATE_TO_CANCEL_MESSAGE, viewModel.state.value.actionError)
        assertEquals(0, fixture.bookings.writeCount)
    }

    /** "El Cliente cancela justo 3 horas antes de la cita": the window is inclusive. */
    @Test
    fun `exactly 3 hours before the appointment cancelling still works`() = runTest(testDispatcher) {
        confirmedAt(hour = 14)
        val viewModel = fixture.detailViewModel()
        start(viewModel, "b1")
        assertTrue(viewModel.state.value.canCancel)

        send(
            viewModel,
            BookingDetailAction.OpenCancelForm,
            BookingDetailAction.ReasonSelected(CustomerReasonCode.NO_LONGER_NEEDED),
            BookingDetailAction.ConfirmCancel,
        )

        assertEquals(BookingStatus.CANCELLED_BY_CUSTOMER, fixture.bookings.stored("b1").status)
    }

    /** The window closes while the picker is open: the domain refuses and the screen says so. */
    @Test
    fun `the domain refuses a cancellation that became late while the picker was open`() = runTest(testDispatcher) {
        confirmedAt(hour = 14, minute = 30)
        val viewModel = fixture.detailViewModel()
        start(viewModel, "b1")
        send(
            viewModel,
            BookingDetailAction.OpenCancelForm,
            BookingDetailAction.ReasonSelected(CustomerReasonCode.CHANGE_OF_PLANS),
        )

        fixture.clock.instant = BookingFixture.NOW + 1.hours
        send(viewModel, BookingDetailAction.ConfirmCancel)

        val state = viewModel.state.value
        assertEquals(Booking.TOO_LATE_TO_CANCEL_MESSAGE, state.actionError)
        assertNull(state.cancelForm)
        assertEquals(BookingStatus.CONFIRMED, state.booking?.status)
        assertEquals(0, fixture.bookings.writeCount)
        assertFalse(state.canCancel)
    }

    @Test
    fun `only a confirmed booking can be cancelled`() = runTest(testDispatcher) {
        fixture.booking("b1", BookingStatus.REQUESTED)
        val viewModel = fixture.detailViewModel()
        start(viewModel, "b1")

        send(viewModel, BookingDetailAction.OpenCancelForm)

        assertNull(viewModel.state.value.cancelForm)
        assertEquals(0, fixture.bookings.writeCount)
    }

    // --- C10 rejected / C10b expired -----------------------------------------------------------

    @Test
    fun `a rejected booking shows the provider's reason and the ways on`() = runTest(testDispatcher) {
        fixture.bookings.seed(
            fixture.booking("b1", BookingStatus.REJECTED).copy(
                reason = Reason(ProviderReasonCode.OUTSIDE_SERVICE_AREA, "Muy lejos"),
            ),
        )
        val viewModel = fixture.detailViewModel()
        start(viewModel, "b1")

        val reason = viewModel.state.value.booking?.reason
        assertEquals(BookingDetailVariant.REJECTED, viewModel.state.value.variant)
        assertEquals("Fuera de la zona de servicio", reason?.label())
        assertEquals("Muy lejos", reason?.note)

        assertEquals(BookingDetailEvent.Rebook("s-local"), sendAndAwaitEvent(viewModel, BookingDetailAction.RebookClicked))
        assertEquals(BookingDetailEvent.GoHome, sendAndAwaitEvent(viewModel, BookingDetailAction.GoHomeClicked))
    }

    @Test
    fun `an expired booking is the expired variant with the same ways on`() = runTest(testDispatcher) {
        fixture.booking("b1", BookingStatus.EXPIRED, requestedAt = BookingFixture.NOW - 30.hours)
        val viewModel = fixture.detailViewModel()
        start(viewModel, "b1")

        assertEquals(BookingDetailVariant.EXPIRED, viewModel.state.value.variant)
        assertEquals(BookingDetailEvent.Rebook("s-local"), sendAndAwaitEvent(viewModel, BookingDetailAction.RebookClicked))
    }

    // --- completed, cancelled ------------------------------------------------------------------

    @Test
    fun `a completed unrated booking offers to rate`() = runTest(testDispatcher) {
        fixture.booking("b1", BookingStatus.COMPLETED)
        val viewModel = fixture.detailViewModel()
        start(viewModel, "b1")

        assertEquals(BookingDetailVariant.COMPLETED, viewModel.state.value.variant)
        assertTrue(viewModel.state.value.canRate)
        assertEquals(BookingDetailEvent.Rate("b1"), sendAndAwaitEvent(viewModel, BookingDetailAction.RateClicked))
    }

    @Test
    fun `a rated or not completed booking does not offer to rate`() = runTest(testDispatcher) {
        fixture.bookings.seed(fixture.booking("rated", BookingStatus.COMPLETED).copy(rating = Rating(5, "Genial")))
        fixture.booking("open", BookingStatus.CONFIRMED)
        val viewModel = fixture.detailViewModel()

        start(viewModel, "rated")
        assertFalse(viewModel.state.value.canRate)
        start(viewModel, "open")
        assertFalse(viewModel.state.value.canRate)
    }

    @Test
    fun `a cancelled booking keeps its reason`() = runTest(testDispatcher) {
        fixture.bookings.seed(
            fixture.booking("b1", BookingStatus.CANCELLED_BY_PROVIDER).copy(
                reason = Reason(ProviderReasonCode.SERVICE_TEMPORARILY_UNAVAILABLE, null),
            ),
        )
        val viewModel = fixture.detailViewModel()
        start(viewModel, "b1")

        assertEquals(BookingDetailVariant.CANCELLED, viewModel.state.value.variant)
        assertEquals("Servicio no disponible temporalmente", viewModel.state.value.booking?.reason?.label())
    }

    // --- load, ownership, re-entry -------------------------------------------------------------

    @Test
    fun `a missing booking is not found`() = runTest(testDispatcher) {
        val viewModel = fixture.detailViewModel()

        start(viewModel, "nope")

        assertEquals(DomainError.NotFound, viewModel.state.value.error)
        assertNull(viewModel.state.value.booking)
    }

    @Test
    fun `cancelling someone else's booking is refused`() = runTest(testDispatcher) {
        fixture.booking("b1", BookingStatus.CONFIRMED, LocalDateTime(2026, 10, 12, 10, 0), customerId = "other")
        val viewModel = fixture.detailViewModel()
        start(viewModel, "b1")
        send(
            viewModel,
            BookingDetailAction.OpenCancelForm,
            BookingDetailAction.ReasonSelected(CustomerReasonCode.CHANGE_OF_PLANS),
            BookingDetailAction.ConfirmCancel,
        )

        assertEquals("No autorizado", viewModel.state.value.actionError)
        assertEquals(BookingStatus.CONFIRMED, fixture.bookings.stored("b1").status)
    }

    /** Re-entry: the ViewModel is shared, so Start for another Booking must wipe the previous one. */
    @Test
    fun `starting another booking replaces the previous one and its open picker`() = runTest(testDispatcher) {
        fixture.booking("b1", BookingStatus.CONFIRMED, LocalDateTime(2026, 10, 12, 10, 0))
        fixture.booking("b2", BookingStatus.REQUESTED)
        val viewModel = fixture.detailViewModel()
        start(viewModel, "b1")
        send(viewModel, BookingDetailAction.OpenCancelForm)

        viewModel.onAction(BookingDetailAction.Start("b2"))
        assertNull(viewModel.state.value.booking)
        assertNull(viewModel.state.value.cancelForm)
        assertTrue(viewModel.state.value.isLoading)

        advanceUntilIdle()
        assertEquals("b2", viewModel.state.value.booking?.id)
        assertEquals(BookingDetailVariant.PENDING, viewModel.state.value.variant)
    }

    @Test
    fun `entering again re-reads a status that changed elsewhere`() = runTest(testDispatcher) {
        fixture.booking("b1", BookingStatus.REQUESTED)
        val viewModel = fixture.detailViewModel()
        start(viewModel, "b1")
        assertEquals(BookingDetailVariant.PENDING, viewModel.state.value.variant)

        fixture.bookings.seed(fixture.bookings.stored("b1").copy(status = BookingStatus.CONFIRMED))
        start(viewModel, "b1")

        assertEquals(BookingDetailVariant.CONFIRMED, viewModel.state.value.variant)
    }

    @Test
    fun `back closes an open picker first and then leaves`() = runTest(testDispatcher) {
        confirmedAt(hour = 18)
        val viewModel = fixture.detailViewModel()
        start(viewModel, "b1")
        send(viewModel, BookingDetailAction.OpenCancelForm)

        send(viewModel, BookingDetailAction.BackClicked)
        assertNull(viewModel.state.value.cancelForm)

        assertEquals(BookingDetailEvent.Finished, sendAndAwaitEvent(viewModel, BookingDetailAction.BackClicked))
    }
}
