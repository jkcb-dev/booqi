@file:OptIn(ExperimentalCoroutinesApi::class)

package corp.khin.solutions.booqi.feature.booking

import corp.khin.solutions.booqi.core.common.DomainError
import corp.khin.solutions.booqi.domain.model.BookingStatus
import corp.khin.solutions.booqi.domain.model.User
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
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Customer flow Grupo 2 (address present / missing) and Grupo 3 (requesting a reservation, the
 * Identity guard) on C5 + C7, through [BookingFlowViewModel]'s reducer over the real use cases:
 * state and events out, for actions in. Time is Monday 5 October 2026 11:00 UTC; the Provider
 * works Mondays 09:00-13:00 in 60-minute slots, so today only 12:00 is still ahead.
 */
class BookingFlowViewModelTest {

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

    private fun TestScope.send(viewModel: BookingFlowViewModel, vararg actions: BookingFlowAction) {
        actions.forEach { viewModel.onAction(it) }
        advanceUntilIdle()
    }

    private suspend fun TestScope.sendAndAwaitEvent(
        viewModel: BookingFlowViewModel,
        vararg actions: BookingFlowAction,
    ): BookingFlowEvent {
        var event: BookingFlowEvent? = null
        val collector = launch { event = viewModel.events.first() }
        send(viewModel, *actions)
        collector.join()
        return checkNotNull(event) { "No event received for ${actions.toList()}" }
    }

    /** Opens [serviceId], goes to next Monday and picks its [hour] slot. */
    private fun TestScope.pickNextMondaySlot(
        viewModel: BookingFlowViewModel,
        serviceId: String,
        hour: Int,
    ) {
        send(viewModel, BookingFlowAction.Start(serviceId))
        repeat(DAYS_TO_NEXT_MONDAY) { viewModel.onAction(BookingFlowAction.NextDay) }
        advanceUntilIdle()
        send(viewModel, BookingFlowAction.SlotSelected(BookingFixture.slot(BookingFixture.NEXT_MONDAY, hour)))
    }

    // --- C5: slot selection --------------------------------------------------------------------

    @Test
    fun `start loads the service and today's slots with the past ones unavailable`() = runTest(testDispatcher) {
        val viewModel = fixture.flowViewModel()

        send(viewModel, BookingFlowAction.Start("s-local"))

        val state = viewModel.state.value
        assertFalse(state.isLoading)
        assertEquals("Corte clásico", state.detail?.service?.title)
        assertEquals(BookingFixture.TODAY, state.date)
        assertEquals(
            listOf(9 to false, 10 to false, 11 to false, 12 to true),
            state.slotCells.map { it.slot.start.hour to it.isAvailable },
        )
        assertEquals(BookingStep.SELECT_SLOT, state.step)
    }

    @Test
    fun `start shows loading until the service arrives`() = runTest(testDispatcher) {
        val viewModel = fixture.flowViewModel()

        viewModel.onAction(BookingFlowAction.Start("s-local"))

        assertTrue(viewModel.state.value.isLoading)
        assertNull(viewModel.state.value.detail)
    }

    @Test
    fun `a missing service is reported as not found`() = runTest(testDispatcher) {
        val viewModel = fixture.flowViewModel()

        send(viewModel, BookingFlowAction.Start("nope"))

        assertEquals(DomainError.NotFound, viewModel.state.value.loadError)
        assertNull(viewModel.state.value.detail)
    }

    /** "El TimeSlot deja de estar disponible para otros Clientes mientras la solicitud está pendiente". */
    @Test
    fun `a slot held by another booking is shown as unavailable`() = runTest(testDispatcher) {
        fixture.booking("held", BookingStatus.REQUESTED, LocalDateTime(2026, 10, 12, 10, 0), customerId = "other")
        val viewModel = fixture.flowViewModel()
        send(viewModel, BookingFlowAction.Start("s-local"))

        repeat(DAYS_TO_NEXT_MONDAY) { viewModel.onAction(BookingFlowAction.NextDay) }
        advanceUntilIdle()

        val cells = viewModel.state.value.slotCells
        assertEquals(BookingFixture.NEXT_MONDAY, viewModel.state.value.date)
        assertEquals(listOf(9 to true, 10 to false, 11 to true, 12 to true), cells.map { it.slot.start.hour to it.isAvailable })
    }

    @Test
    fun `a day without schedule has no slots and the date can move both ways`() = runTest(testDispatcher) {
        val viewModel = fixture.flowViewModel()
        send(viewModel, BookingFlowAction.Start("s-local"))
        assertFalse(viewModel.state.value.canGoToPreviousDay)

        send(viewModel, BookingFlowAction.NextDay)

        assertTrue(viewModel.state.value.slotCells.isEmpty())
        assertTrue(viewModel.state.value.canGoToPreviousDay)
        send(viewModel, BookingFlowAction.PreviousDay)
        assertEquals(BookingFixture.TODAY, viewModel.state.value.date)
        assertEquals(4, viewModel.state.value.slotCells.size)
    }

    @Test
    fun `the date cannot go before today`() = runTest(testDispatcher) {
        val viewModel = fixture.flowViewModel()
        send(viewModel, BookingFlowAction.Start("s-local"))

        send(viewModel, BookingFlowAction.PreviousDay)

        assertEquals(BookingFixture.TODAY, viewModel.state.value.date)
    }

    @Test
    fun `only an available slot can be selected`() = runTest(testDispatcher) {
        val viewModel = fixture.flowViewModel()
        send(viewModel, BookingFlowAction.Start("s-local"))

        send(viewModel, BookingFlowAction.SlotSelected(BookingFixture.slot(BookingFixture.TODAY, 9)))
        assertNull(viewModel.state.value.selectedSlot)

        send(viewModel, BookingFlowAction.SlotSelected(BookingFixture.slot(BookingFixture.TODAY, 12)))
        assertEquals(BookingFixture.slot(BookingFixture.TODAY, 12), viewModel.state.value.selectedSlot)
    }

    @Test
    fun `continue needs a selected slot`() = runTest(testDispatcher) {
        val viewModel = fixture.flowViewModel()
        send(viewModel, BookingFlowAction.Start("s-local"))

        send(viewModel, BookingFlowAction.ContinueClicked)

        assertEquals(BookingStep.SELECT_SLOT, viewModel.state.value.step)
    }

    // --- C7: request ---------------------------------------------------------------------------

    /** "Un Cliente solicita una reserva": REQUESTED, snapshots, trimmed note, the name snapshot. */
    @Test
    fun `sending a local request creates a pending booking and reports it`() = runTest(testDispatcher) {
        val viewModel = fixture.flowViewModel()
        pickNextMondaySlot(viewModel, "s-local", hour = 9)
        send(viewModel, BookingFlowAction.ContinueClicked, BookingFlowAction.NoteChanged("  Puerta azul  "))

        val event = sendAndAwaitEvent(viewModel, BookingFlowAction.Submit)

        val created = fixture.bookings.all.single()
        assertEquals(BookingFlowEvent.Done(created.id), event)
        assertEquals(BookingStatus.REQUESTED, created.status)
        assertEquals("Puerta azul", created.customerNote)
        assertEquals("Ana", created.customerNameSnapshot)
        assertEquals(LocalDateTime(2026, 10, 12, 9, 0), created.scheduledAt)
        assertEquals(3500, created.priceCentsSnapshot)
        assertEquals(60, created.durationMinutesSnapshot)
        assertNull(created.deliveryAddress)
        assertFalse(viewModel.state.value.isSubmitting)
    }

    @Test
    fun `an empty note is stored as none`() = runTest(testDispatcher) {
        val viewModel = fixture.flowViewModel()
        pickNextMondaySlot(viewModel, "s-local", hour = 9)
        send(viewModel, BookingFlowAction.ContinueClicked, BookingFlowAction.NoteChanged("   "))

        sendAndAwaitEvent(viewModel, BookingFlowAction.Submit)

        assertNull(fixture.bookings.all.single().customerNote)
    }

    /** Grupo 2, escenario 2: "ya tiene dirección guardada ... se usa automáticamente". */
    @Test
    fun `a domicilio request uses the saved address without asking again`() = runTest(testDispatcher) {
        fixture.users.signIn(BookingFixture.CUSTOMER.copy(address = BookingFixture.HOME_ADDRESS))
        val viewModel = fixture.flowViewModel()
        pickNextMondaySlot(viewModel, "s-home", hour = 9)

        send(viewModel, BookingFlowAction.ContinueClicked)

        assertEquals(AddressLookup.FOUND, viewModel.state.value.addressLookup)
        assertTrue(viewModel.state.value.wantsDelivery)
        assertTrue(viewModel.state.value.canSubmit)
        sendAndAwaitEvent(viewModel, BookingFlowAction.Submit)
        assertEquals(BookingFixture.HOME_ADDRESS, fixture.bookings.all.single().deliveryAddress)
    }

    /** Grupo 2, escenario 1: without a saved address the request is blocked and asks for one. */
    @Test
    fun `a domicilio request without a saved address is blocked and offers to add one`() = runTest(testDispatcher) {
        val viewModel = fixture.flowViewModel()
        pickNextMondaySlot(viewModel, "s-home", hour = 9)

        send(viewModel, BookingFlowAction.ContinueClicked)
        assertEquals(AddressLookup.MISSING, viewModel.state.value.addressLookup)
        assertTrue(viewModel.state.value.isAddressBlocking)
        assertFalse(viewModel.state.value.canSubmit)

        send(viewModel, BookingFlowAction.Submit)
        assertEquals(0, fixture.bookings.writeCount)

        assertEquals(BookingFlowEvent.AddAddress, sendAndAwaitEvent(viewModel, BookingFlowAction.AddAddressClicked))
    }

    @Test
    fun `coming back from adding the address resumes the flow with the chosen slot`() = runTest(testDispatcher) {
        val viewModel = fixture.flowViewModel()
        pickNextMondaySlot(viewModel, "s-home", hour = 10)
        send(viewModel, BookingFlowAction.ContinueClicked, BookingFlowAction.NoteChanged("Timbre roto"))
        sendAndAwaitEvent(viewModel, BookingFlowAction.AddAddressClicked)
        fixture.users.signIn(BookingFixture.CUSTOMER.copy(address = BookingFixture.HOME_ADDRESS))

        send(viewModel, BookingFlowAction.Start("s-home"))

        val state = viewModel.state.value
        assertEquals(BookingStep.CONFIRM, state.step)
        assertEquals(BookingFixture.slot(BookingFixture.NEXT_MONDAY, 10), state.selectedSlot)
        assertEquals("Timbre roto", state.note)
        assertEquals(AddressLookup.FOUND, state.addressLookup)
        assertTrue(state.canSubmit)
    }

    @Test
    fun `a local and domicilio service is local until the customer chooses delivery`() = runTest(testDispatcher) {
        fixture.users.signIn(BookingFixture.CUSTOMER.copy(address = BookingFixture.HOME_ADDRESS))
        val viewModel = fixture.flowViewModel()
        pickNextMondaySlot(viewModel, "s-both", hour = 9)
        send(viewModel, BookingFlowAction.ContinueClicked)
        assertFalse(viewModel.state.value.wantsDelivery)
        assertTrue(viewModel.state.value.canSubmit)

        send(viewModel, BookingFlowAction.AtHomeChanged(true))
        assertTrue(viewModel.state.value.wantsDelivery)
        sendAndAwaitEvent(viewModel, BookingFlowAction.Submit)

        assertEquals(BookingFixture.HOME_ADDRESS, fixture.bookings.all.single().deliveryAddress)
    }

    @Test
    fun `a local and domicilio request at the local needs no address`() = runTest(testDispatcher) {
        val viewModel = fixture.flowViewModel()
        pickNextMondaySlot(viewModel, "s-both", hour = 9)
        send(viewModel, BookingFlowAction.ContinueClicked)

        assertEquals(AddressLookup.MISSING, viewModel.state.value.addressLookup)
        assertTrue(viewModel.state.value.canSubmit)
        sendAndAwaitEvent(viewModel, BookingFlowAction.Submit)
        assertNull(fixture.bookings.all.single().deliveryAddress)
    }

    // --- Identity guard ------------------------------------------------------------------------

    /** "Un visitante intenta reservar sin cuenta": a sign-in state and callback, no Booking, no crash. */
    @Test
    fun `a visitor sending is asked to sign in and nothing is created`() = runTest(testDispatcher) {
        fixture.users.signedIn = null
        val viewModel = fixture.flowViewModel()
        pickNextMondaySlot(viewModel, "s-local", hour = 9)
        send(viewModel, BookingFlowAction.ContinueClicked)
        assertEquals(AddressLookup.SIGNED_OUT, viewModel.state.value.addressLookup)

        val event = sendAndAwaitEvent(viewModel, BookingFlowAction.Submit)

        assertEquals(BookingFlowEvent.SignInRequired, event)
        assertEquals(AccessIssue.SIGN_IN_REQUIRED, viewModel.state.value.access)
        assertFalse(viewModel.state.value.isSubmitting)
        assertEquals(0, fixture.bookings.writeCount)
    }

    /** "...al terminar vuelve a la reserva que estaba haciendo, con el TimeSlot elegido". */
    @Test
    fun `after signing in the visitor returns to the same request with the slot kept`() = runTest(testDispatcher) {
        fixture.users.signedIn = null
        val viewModel = fixture.flowViewModel()
        pickNextMondaySlot(viewModel, "s-local", hour = 11)
        send(viewModel, BookingFlowAction.ContinueClicked)
        sendAndAwaitEvent(viewModel, BookingFlowAction.Submit)
        fixture.users.signIn(BookingFixture.CUSTOMER)

        send(viewModel, BookingFlowAction.Start("s-local"))

        val state = viewModel.state.value
        assertNull(state.access)
        assertEquals(BookingStep.CONFIRM, state.step)
        assertEquals(BookingFixture.slot(BookingFixture.NEXT_MONDAY, 11), state.selectedSlot)
        val event = sendAndAwaitEvent(viewModel, BookingFlowAction.Submit)
        assertIs<BookingFlowEvent.Done>(event)
        assertEquals(1, fixture.bookings.all.size)
    }

    /** "Solicitar una reserva exige una cuenta verificada": unverified email -> verify state. */
    @Test
    fun `an unverified email is asked to verify and nothing is created`() = runTest(testDispatcher) {
        fixture.users.signIn(User("customer-1", "Ana", "ana@example.com", isEmailVerified = false))
        val viewModel = fixture.flowViewModel()
        pickNextMondaySlot(viewModel, "s-local", hour = 9)
        send(viewModel, BookingFlowAction.ContinueClicked)

        val event = sendAndAwaitEvent(viewModel, BookingFlowAction.Submit)

        assertEquals(BookingFlowEvent.SignInRequired, event)
        assertEquals(AccessIssue.EMAIL_NOT_VERIFIED, viewModel.state.value.access)
        assertEquals(0, fixture.bookings.writeCount)
    }

    @Test
    fun `the access notice button hands off to sign in again`() = runTest(testDispatcher) {
        val viewModel = fixture.flowViewModel()
        send(viewModel, BookingFlowAction.Start("s-local"))

        assertEquals(BookingFlowEvent.SignInRequired, sendAndAwaitEvent(viewModel, BookingFlowAction.SignInClicked))
    }

    // --- slot taken meanwhile ------------------------------------------------------------------

    @Test
    fun `a slot taken meanwhile sends the customer back to pick another with a message`() = runTest(testDispatcher) {
        val viewModel = fixture.flowViewModel()
        pickNextMondaySlot(viewModel, "s-local", hour = 9)
        send(viewModel, BookingFlowAction.ContinueClicked)
        fixture.booking("taken", BookingStatus.CONFIRMED, LocalDateTime(2026, 10, 12, 9, 0), customerId = "other")

        send(viewModel, BookingFlowAction.Submit)

        val state = viewModel.state.value
        assertEquals(BookingStep.SELECT_SLOT, state.step)
        assertNull(state.selectedSlot)
        assertNotNull(state.submitError)
        assertFalse(state.slotCells.first { it.slot.start.hour == 9 }.isAvailable)
        assertFalse(state.isSubmitting)
        assertEquals(0, fixture.bookings.writeCount)
    }

    // --- navigation and re-entry ---------------------------------------------------------------

    @Test
    fun `back from the confirmation keeps the selection and back from the grid leaves`() = runTest(testDispatcher) {
        val viewModel = fixture.flowViewModel()
        pickNextMondaySlot(viewModel, "s-local", hour = 9)
        send(viewModel, BookingFlowAction.ContinueClicked)

        send(viewModel, BookingFlowAction.BackClicked)
        assertEquals(BookingStep.SELECT_SLOT, viewModel.state.value.step)
        assertNotNull(viewModel.state.value.selectedSlot)

        assertEquals(BookingFlowEvent.Finished, sendAndAwaitEvent(viewModel, BookingFlowAction.BackClicked))
        assertNull(viewModel.state.value.selectedSlot)
    }

    /** Re-entry: the ViewModel is shared, so entering again must not show the previous flow. */
    @Test
    fun `entering the same service again starts a clean flow`() = runTest(testDispatcher) {
        val viewModel = fixture.flowViewModel()
        pickNextMondaySlot(viewModel, "s-local", hour = 9)
        send(viewModel, BookingFlowAction.ContinueClicked, BookingFlowAction.NoteChanged("vieja nota"))

        send(viewModel, BookingFlowAction.Start("s-local"))

        val state = viewModel.state.value
        assertEquals(BookingStep.SELECT_SLOT, state.step)
        assertNull(state.selectedSlot)
        assertEquals("", state.note)
        assertEquals(BookingFixture.TODAY, state.date)
    }

    @Test
    fun `starting another service replaces the previous one`() = runTest(testDispatcher) {
        val viewModel = fixture.flowViewModel()
        send(viewModel, BookingFlowAction.Start("s-local"))

        viewModel.onAction(BookingFlowAction.Start("s-home"))
        assertNull(viewModel.state.value.detail)
        assertTrue(viewModel.state.value.isLoading)
        assertEquals("s-home", viewModel.state.value.serviceId)

        advanceUntilIdle()
        assertEquals("Limpieza profunda", viewModel.state.value.detail?.service?.title)
    }

    @Test
    fun `after a sent request the next entry starts clean`() = runTest(testDispatcher) {
        val viewModel = fixture.flowViewModel()
        pickNextMondaySlot(viewModel, "s-local", hour = 9)
        send(viewModel, BookingFlowAction.ContinueClicked)
        sendAndAwaitEvent(viewModel, BookingFlowAction.Submit)

        send(viewModel, BookingFlowAction.Start("s-local"))

        assertNull(viewModel.state.value.selectedSlot)
        assertEquals(BookingStep.SELECT_SLOT, viewModel.state.value.step)
    }

    private companion object {
        /** Monday 5 October to Monday 12 October. */
        const val DAYS_TO_NEXT_MONDAY = 7
    }
}
