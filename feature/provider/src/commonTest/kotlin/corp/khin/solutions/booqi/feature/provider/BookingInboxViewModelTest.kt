@file:OptIn(ExperimentalCoroutinesApi::class)

package corp.khin.solutions.booqi.feature.provider

import corp.khin.solutions.booqi.core.common.DomainError
import corp.khin.solutions.booqi.domain.model.Booking
import corp.khin.solutions.booqi.domain.model.BookingStatus
import corp.khin.solutions.booqi.domain.model.ProviderReasonCode
import corp.khin.solutions.booqi.domain.model.ServiceDetails
import corp.khin.solutions.booqi.domain.model.ServiceModality
import corp.khin.solutions.booqi.domain.usecase.AceptarReservaUseCase
import corp.khin.solutions.booqi.domain.usecase.CancelarReservaAceptadaUseCase
import corp.khin.solutions.booqi.domain.usecase.CompletarCitaUseCase
import corp.khin.solutions.booqi.domain.usecase.ObtenerReservaUseCase
import corp.khin.solutions.booqi.domain.usecase.ObtenerReservasDelProveedorUseCase
import corp.khin.solutions.booqi.domain.usecase.ObtenerServiciosDelProveedorUseCase
import corp.khin.solutions.booqi.domain.usecase.ObtenerSolicitudesPendientesUseCase
import corp.khin.solutions.booqi.domain.usecase.RechazarReservaUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

/** A [Clock] the test moves by hand. */
private class BookingTestClock(var instant: Instant) : Clock {
    override fun now(): Instant = instant
}

/**
 * Grupo 4 scenarios (docs/domain/provider-flow.md) that surface on the P8/P9/P10 inbox, exercised
 * through [BookingInboxViewModel]'s reducer: state and events out, for actions in. The fake clock
 * and the UTC zone make "the appointment's end time has passed" deterministic.
 */
@Suppress("LargeClass")
class BookingInboxViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val bookings = FakeBookingRepository()
    private val services = FakeServiceRepository()

    // Saturday 3 October 2026, midday UTC (the provider's wall clock too: the tests use TimeZone.UTC).
    private val now = Instant.parse("2026-10-03T12:00:00Z")
    private val clock = BookingTestClock(now)

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun newViewModel() = BookingInboxViewModel(
        queries = BookingInboxQueries(
            pendingRequests = ObtenerSolicitudesPendientesUseCase(bookings, clock),
            agenda = ObtenerReservasDelProveedorUseCase(bookings),
            booking = ObtenerReservaUseCase(bookings),
            services = ObtenerServiciosDelProveedorUseCase(services),
        ),
        commands = BookingInboxCommands(
            accept = AceptarReservaUseCase(bookings, clock),
            reject = RechazarReservaUseCase(bookings, clock),
            complete = CompletarCitaUseCase(bookings, clock),
            cancel = CancelarReservaAceptadaUseCase(bookings),
        ),
        clock = clock,
        timeZone = TimeZone.UTC,
    )

    /** The ViewModel plus the Start [BookingRequestInboxScreen] sends on every entry, settled. */
    private fun started(): BookingInboxViewModel =
        newViewModel().also {
            it.onAction(BookingInboxAction.Start)
            testDispatcher.scheduler.advanceUntilIdle()
        }

    private fun booking(
        id: String,
        status: BookingStatus = BookingStatus.REQUESTED,
        scheduledAt: LocalDateTime = LocalDateTime(2026, 10, 4, 10, 0),
        requestedAt: Instant = now - 1.hours,
        providerId: String = TEMPORARY_PROVIDER_ID,
        customerNote: String? = null,
    ) = Booking(
        id = id,
        providerId = providerId,
        serviceId = "service-1",
        customerId = "customer-1",
        scheduledAt = scheduledAt,
        durationMinutesSnapshot = 60,
        priceCentsSnapshot = 1250,
        requestedAt = requestedAt,
        status = status,
        customerNote = customerNote,
    )

    private suspend fun seedService() {
        services.addService(
            TEMPORARY_PROVIDER_ID,
            ServiceDetails("Corte de pelo", "https://example.com/c.jpg", "Corte", 1250, 60, ServiceModality.LOCAL),
        )
    }

    private fun BookingInboxViewModel.send(vararg actions: BookingInboxAction) {
        actions.forEach { onAction(it) }
        testDispatcher.scheduler.advanceUntilIdle()
    }

    private fun BookingInboxViewModel.rejectWith(code: ProviderReasonCode, bookingId: String, note: String? = null) {
        send(BookingInboxAction.OpenBooking(bookingId), BookingInboxAction.OpenReasonForm(ReasonIntent.REJECT))
        send(BookingInboxAction.ReasonSelected(code))
        if (note != null) send(BookingInboxAction.ReasonNoteChanged(note))
        send(BookingInboxAction.ConfirmReason)
    }

    // --- P8: the inbox ----------------------------------------------------------------------

    /** Escenario: "El Proveedor consulta su bandeja de solicitudes pendientes". */
    @Test
    fun `inbox lists only answerable pending requests of the provider oldest first`() = runTest(testDispatcher) {
        seedService()
        bookings.seed(booking("newer", requestedAt = now - 1.hours))
        bookings.seed(booking("older", requestedAt = now - 5.hours))
        bookings.seed(booking("lapsed", requestedAt = now - 25.hours))
        bookings.seed(booking("confirmed", status = BookingStatus.CONFIRMED))
        bookings.seed(booking("other-provider", providerId = "otro"))

        val viewModel = started()

        val state = viewModel.state.value
        assertFalse(state.isLoading)
        assertNull(state.loadError)
        assertEquals(BookingInboxTab.REQUESTS, state.tab)
        assertEquals(listOf("older", "newer"), state.requests.map { it.id })
        assertEquals(listOf("confirmed"), state.confirmed.map { it.id })
        assertEquals("Corte de pelo", state.serviceTitles["service-1"])
    }

    @Test
    fun `a provider without requests gets empty lists and no error`() = runTest(testDispatcher) {
        val viewModel = started()

        val state = viewModel.state.value
        assertTrue(state.requests.isEmpty())
        assertTrue(state.confirmed.isEmpty())
        assertNull(state.loadError)
        assertFalse(state.isLoading)
    }

    @Test
    fun `entering the screen again reloads drops the open detail and returns to the first tab`() =
        runTest(testDispatcher) {
            bookings.seed(booking("first"))
            val viewModel = started()
            viewModel.send(BookingInboxAction.SelectTab(BookingInboxTab.CONFIRMED), BookingInboxAction.OpenBooking("first"))
            bookings.seed(booking("second", requestedAt = now - 30.minutes))

            viewModel.send(BookingInboxAction.Start)

            val state = viewModel.state.value
            assertEquals(listOf("first", "second"), state.requests.map { it.id })
            assertNull(state.detail)
            assertEquals(BookingInboxTab.REQUESTS, state.tab)
        }

    @Test
    fun `a failing load exposes the error and Refresh recovers`() = runTest(testDispatcher) {
        bookings.seed(booking("one"))
        bookings.listFailure = DomainError.NoConnection
        val viewModel = started()

        assertEquals(DomainError.NoConnection, viewModel.state.value.loadError)
        assertFalse(viewModel.state.value.isLoading)
        assertTrue(viewModel.state.value.requests.isEmpty())

        bookings.listFailure = null
        viewModel.send(BookingInboxAction.Refresh)

        assertNull(viewModel.state.value.loadError)
        assertEquals(listOf("one"), viewModel.state.value.requests.map { it.id })
    }

    // --- P9: request detail + accept ----------------------------------------------------------

    @Test
    fun `opening a request shows its detail with the customer's note and the fresh copy`() = runTest(testDispatcher) {
        bookings.seed(booking("one", customerNote = "Llego 5 min tarde"))
        val viewModel = started()
        bookings.seed(booking("one", customerNote = "Llego 10 min tarde"))

        viewModel.send(BookingInboxAction.OpenBooking("one"))

        val detail = assertNotNull(viewModel.state.value.detail)
        assertEquals("Llego 10 min tarde", detail.booking.customerNote)
        assertNull(detail.reasonForm)
    }

    /** Escenario: "El Proveedor acepta una solicitud". */
    @Test
    fun `accepting a request confirms it closes the detail and moves it to Confirmadas`() =
        runTest(testDispatcher) {
            bookings.seed(booking("one"))
            val viewModel = started()
            viewModel.send(BookingInboxAction.OpenBooking("one"))

            viewModel.send(BookingInboxAction.Accept("one"))

            assertEquals(BookingStatus.CONFIRMED, bookings.stored("one").status)
            val state = viewModel.state.value
            assertNull(state.detail)
            assertEquals(BookingNotice("Solicitud aceptada"), state.notice)
            assertTrue(state.requests.isEmpty())
            assertEquals(listOf("one"), state.confirmed.map { it.id })
            assertNull(state.submittingBookingId)
        }

    @Test
    fun `accepting straight from the P8 row works without opening the detail`() = runTest(testDispatcher) {
        bookings.seed(booking("one"))
        val viewModel = started()

        viewModel.send(BookingInboxAction.Accept("one"))

        assertEquals(BookingStatus.CONFIRMED, bookings.stored("one").status)
        assertEquals("Solicitud aceptada", viewModel.state.value.notice?.message)
    }

    // --- P9: reject with a reason -------------------------------------------------------------

    /** Escenario: "El Proveedor rechaza una solicitud" (motivo predefinido). */
    @Test
    fun `rejecting with a predefined reason stores it and frees the request`() = runTest(testDispatcher) {
        bookings.seed(booking("one"))
        val viewModel = started()

        viewModel.rejectWith(ProviderReasonCode.OUTSIDE_SERVICE_AREA, "one")

        val stored = bookings.stored("one")
        assertEquals(BookingStatus.REJECTED, stored.status)
        assertEquals(ProviderReasonCode.OUTSIDE_SERVICE_AREA, stored.reason?.code)
        assertNull(stored.reason?.note)
        assertEquals("Solicitud rechazada", viewModel.state.value.notice?.message)
        assertNull(viewModel.state.value.detail)
        assertTrue(viewModel.state.value.requests.isEmpty())
    }

    /** Escenario: rechazo con "Otro" + texto libre. */
    @Test
    fun `rejecting with Otro sends the free text`() = runTest(testDispatcher) {
        bookings.seed(booking("one"))
        val viewModel = started()

        viewModel.rejectWith(ProviderReasonCode.OTHER, "one", note = "Estoy de viaje")

        val reason = assertNotNull(bookings.stored("one").reason)
        assertEquals(ProviderReasonCode.OTHER, reason.code)
        assertEquals("Estoy de viaje", reason.note)
    }

    @Test
    fun `Otro without text is allowed because the note is optional`() = runTest(testDispatcher) {
        bookings.seed(booking("one"))
        val viewModel = started()

        viewModel.rejectWith(ProviderReasonCode.OTHER, "one")

        assertEquals(BookingStatus.REJECTED, bookings.stored("one").status)
        assertNull(bookings.stored("one").reason?.note)
    }

    @Test
    fun `text typed for Otro is dropped if another reason ends up selected`() = runTest(testDispatcher) {
        bookings.seed(booking("one"))
        val viewModel = started()
        viewModel.send(BookingInboxAction.OpenBooking("one"), BookingInboxAction.OpenReasonForm(ReasonIntent.REJECT))
        viewModel.send(
            BookingInboxAction.ReasonSelected(ProviderReasonCode.OTHER),
            BookingInboxAction.ReasonNoteChanged("texto"),
            BookingInboxAction.ReasonSelected(ProviderReasonCode.NOT_AVAILABLE_AT_THIS_TIME),
            BookingInboxAction.ConfirmReason,
        )

        assertNull(bookings.stored("one").reason?.note)
    }

    /** Escenario: rechazar sin elegir motivo queda bloqueado. */
    @Test
    fun `rejecting without a reason is blocked and nothing is saved`() = runTest(testDispatcher) {
        bookings.seed(booking("one"))
        val viewModel = started()
        viewModel.send(BookingInboxAction.OpenBooking("one"), BookingInboxAction.OpenReasonForm(ReasonIntent.REJECT))

        viewModel.send(BookingInboxAction.ConfirmReason)

        assertEquals(BookingStatus.REQUESTED, bookings.stored("one").status)
        assertEquals(0, bookings.writeCount)
        val form = assertNotNull(viewModel.state.value.detail?.reasonForm)
        assertEquals("Elige un motivo para continuar", form.error)
        assertNotNull(viewModel.state.value.detail)
    }

    @Test
    fun `Rechazar on a P8 row opens the detail with the reason picker showing`() = runTest(testDispatcher) {
        bookings.seed(booking("one"))
        val viewModel = started()

        viewModel.send(BookingInboxAction.OpenBooking("one", ReasonIntent.REJECT))

        assertEquals(ReasonIntent.REJECT, viewModel.state.value.detail?.reasonForm?.intent)
    }

    // --- P10: confirmed appointment -----------------------------------------------------------

    private fun seedConfirmed(id: String = "c1", scheduledAt: LocalDateTime) =
        bookings.seed(booking(id, status = BookingStatus.CONFIRMED, scheduledAt = scheduledAt))

    /** Escenario (UI affordance, Figma P10): Completar disabled until the appointment has ended. */
    @Test
    fun `completing is disabled before the appointment ends and enabled once it has`() = runTest(testDispatcher) {
        seedConfirmed(scheduledAt = LocalDateTime(2026, 10, 3, 12, 30)) // ends 13:30, now is 12:00
        val viewModel = started()
        viewModel.send(BookingInboxAction.OpenBooking("c1"))

        fun canComplete() = viewModel.state.value.let { it.detail!!.booking.hasEnded(it.now, it.timeZone) }
        assertFalse(canComplete())

        viewModel.send(BookingInboxAction.Complete)
        assertEquals(BookingStatus.CONFIRMED, bookings.stored("c1").status)
        assertNotNull(viewModel.state.value.detail?.actionError)

        clock.instant = Instant.parse("2026-10-03T13:29:59Z")
        viewModel.send(BookingInboxAction.Tick)
        assertFalse(canComplete())

        // The exact end instant already counts as ended.
        clock.instant = Instant.parse("2026-10-03T13:30:00Z")
        viewModel.send(BookingInboxAction.Tick)
        assertTrue(canComplete())
    }

    /** Escenario: "El Proveedor marca una cita confirmada como completada". */
    @Test
    fun `completing after the end time stores Completed and closes the detail`() = runTest(testDispatcher) {
        seedConfirmed(scheduledAt = LocalDateTime(2026, 10, 3, 9, 0)) // ended 10:00
        val viewModel = started()
        viewModel.send(BookingInboxAction.OpenBooking("c1"))

        viewModel.send(BookingInboxAction.Complete)

        val stored = bookings.stored("c1")
        assertEquals(BookingStatus.COMPLETED, stored.status)
        assertEquals(now, stored.completedAt)
        val state = viewModel.state.value
        assertNull(state.detail)
        assertEquals("Cita marcada como completada", state.notice?.message)
        assertTrue(state.confirmed.isEmpty())
    }

    @Test
    fun `Complete re-reads the clock instead of trusting a stale now`() = runTest(testDispatcher) {
        seedConfirmed(scheduledAt = LocalDateTime(2026, 10, 3, 12, 30))
        val viewModel = started()
        viewModel.send(BookingInboxAction.OpenBooking("c1"))

        clock.instant = Instant.parse("2026-10-03T14:00:00Z") // no Tick sent
        viewModel.send(BookingInboxAction.Complete)

        assertEquals(BookingStatus.COMPLETED, bookings.stored("c1").status)
    }

    /** Escenario: "El Proveedor cancela una cita ya confirmada". */
    @Test
    fun `cancelling an accepted appointment stores the reason`() = runTest(testDispatcher) {
        seedConfirmed(scheduledAt = LocalDateTime(2026, 10, 5, 9, 0))
        val viewModel = started()
        viewModel.send(BookingInboxAction.OpenBooking("c1"), BookingInboxAction.OpenReasonForm(ReasonIntent.CANCEL))
        viewModel.send(BookingInboxAction.ReasonSelected(ProviderReasonCode.OTHER))
        viewModel.send(BookingInboxAction.ReasonNoteChanged("Imprevisto"), BookingInboxAction.ConfirmReason)

        val stored = bookings.stored("c1")
        assertEquals(BookingStatus.CANCELLED_BY_PROVIDER, stored.status)
        assertEquals(ProviderReasonCode.OTHER, stored.reason?.code)
        assertEquals("Imprevisto", stored.reason?.note)
        assertEquals("Cita cancelada", viewModel.state.value.notice?.message)
        assertTrue(viewModel.state.value.confirmed.isEmpty())
    }

    @Test
    fun `cancelling without a reason is blocked`() = runTest(testDispatcher) {
        seedConfirmed(scheduledAt = LocalDateTime(2026, 10, 5, 9, 0))
        val viewModel = started()
        viewModel.send(BookingInboxAction.OpenBooking("c1"), BookingInboxAction.OpenReasonForm(ReasonIntent.CANCEL))

        viewModel.send(BookingInboxAction.ConfirmReason)

        assertEquals(BookingStatus.CONFIRMED, bookings.stored("c1").status)
        assertEquals(0, bookings.writeCount)
        assertNotNull(viewModel.state.value.detail?.reasonForm?.error)
    }

    // --- invalid transitions ------------------------------------------------------------------

    /** Escenario: "Una solicitud solo puede cambiar de estado por las transiciones permitidas". */
    @Test
    fun `an invalid transition shows the domain message inline and refreshes the stale detail`() =
        runTest(testDispatcher) {
            bookings.seed(booking("one"))
            val viewModel = started()
            viewModel.send(BookingInboxAction.OpenBooking("one"))
            // Meanwhile the request was already accepted elsewhere.
            bookings.seed(booking("one", status = BookingStatus.CONFIRMED))

            viewModel.send(BookingInboxAction.Accept("one"))

            val state = viewModel.state.value
            val detail = assertNotNull(state.detail)
            assertTrue(detail.actionError!!.isNotBlank())
            assertEquals(BookingStatus.CONFIRMED, detail.booking.status)
            assertNull(state.submittingBookingId)
            assertNull(state.notice)
            assertEquals(listOf("one"), state.confirmed.map { it.id })
        }

    /** Escenario: "El Proveedor intenta responder una solicitud vencida". */
    @Test
    fun `answering a request that lapsed while open shows the lapsed message`() = runTest(testDispatcher) {
        bookings.seed(booking("one", requestedAt = now - 23.hours))
        val viewModel = started()
        viewModel.send(BookingInboxAction.OpenBooking("one"))

        clock.instant = now + 2.hours
        viewModel.send(BookingInboxAction.Accept("one"))

        assertEquals(BookingStatus.REQUESTED, bookings.stored("one").status)
        val message = assertNotNull(viewModel.state.value.detail?.actionError)
        assertTrue("venció" in message)
        // The reload hides the lapsed request from the inbox.
        assertTrue(viewModel.state.value.requests.isEmpty())
    }

    @Test
    fun `a failed accept from a P8 row shows an error notice`() = runTest(testDispatcher) {
        bookings.seed(booking("one", requestedAt = now - 23.hours))
        val viewModel = started()

        clock.instant = now + 2.hours
        viewModel.send(BookingInboxAction.Accept("one"))

        val notice = assertNotNull(viewModel.state.value.notice)
        assertTrue(notice.isError)
        assertTrue("venció" in notice.message)
        assertEquals(BookingStatus.REQUESTED, bookings.stored("one").status)
    }

    @Test
    fun `opening a booking that is not in the lists does nothing`() = runTest(testDispatcher) {
        bookings.seed(booking("one"))
        val viewModel = started()

        viewModel.send(BookingInboxAction.OpenBooking("ghost"))

        assertNull(viewModel.state.value.detail)
    }

    // --- navigation inside the screen -----------------------------------------------------------

    @Test
    fun `Volver closes the reason picker then the detail then finishes`() = runTest(testDispatcher) {
        bookings.seed(booking("one"))
        val viewModel = started()
        viewModel.send(BookingInboxAction.OpenBooking("one", ReasonIntent.REJECT))

        viewModel.send(BookingInboxAction.BackClicked)
        assertNotNull(viewModel.state.value.detail)
        assertNull(viewModel.state.value.detail?.reasonForm)

        viewModel.send(BookingInboxAction.BackClicked)
        assertNull(viewModel.state.value.detail)

        var event: BookingInboxEvent? = null
        val collector = launch { event = viewModel.events.first() }
        viewModel.onAction(BookingInboxAction.BackClicked)
        advanceUntilIdle()
        collector.join()
        assertIs<BookingInboxEvent.Finished>(event)
    }

    @Test
    fun `switching tab shows the confirmed list and clears a notice`() = runTest(testDispatcher) {
        bookings.seed(booking("one"))
        seedConfirmed(scheduledAt = LocalDateTime(2026, 10, 5, 9, 0))
        val viewModel = started()
        viewModel.send(BookingInboxAction.Accept("one"))
        assertNotNull(viewModel.state.value.notice)

        viewModel.send(BookingInboxAction.SelectTab(BookingInboxTab.CONFIRMED))

        assertEquals(BookingInboxTab.CONFIRMED, viewModel.state.value.tab)
        assertNull(viewModel.state.value.notice)
        assertEquals(listOf("one", "c1").sorted(), viewModel.state.value.confirmed.map { it.id }.sorted())
    }
}
