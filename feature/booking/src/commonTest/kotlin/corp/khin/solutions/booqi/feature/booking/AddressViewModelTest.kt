@file:OptIn(ExperimentalCoroutinesApi::class)

package corp.khin.solutions.booqi.feature.booking

import corp.khin.solutions.booqi.core.common.DomainError
import corp.khin.solutions.booqi.domain.model.Address
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
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Customer flow Grupo 2 on the address picker (C6): search, pick, add or update, validation. */
class AddressViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val fixture = BookingFixture()
    private val casa = Address("Av. Corrientes 1234, CABA", -34.6037, -58.3816)
    private val oficina = Address("Av. Santa Fe 900, CABA", -34.5956, -58.3777)

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fixture.addressSearch.results = listOf(casa, oficina)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun TestScope.send(viewModel: AddressViewModel, vararg actions: AddressAction) {
        actions.forEach { viewModel.onAction(it) }
        advanceUntilIdle()
    }

    private suspend fun TestScope.sendAndAwaitEvent(viewModel: AddressViewModel, vararg actions: AddressAction): AddressEvent {
        var event: AddressEvent? = null
        val collector = launch { event = viewModel.events.first() }
        send(viewModel, *actions)
        collector.join()
        return checkNotNull(event) { "No event received" }
    }

    @Test
    fun `entering without a saved address shows an empty picker`() = runTest(testDispatcher) {
        val viewModel = fixture.addressViewModel()

        send(viewModel, AddressAction.Start)

        val state = viewModel.state.value
        assertFalse(state.isLoading)
        assertNull(state.savedAddress)
        assertFalse(state.canSave)
    }

    @Test
    fun `entering with a saved address shows it as the current one`() = runTest(testDispatcher) {
        fixture.users.signIn(BookingFixture.CUSTOMER.copy(address = casa))
        val viewModel = fixture.addressViewModel()

        send(viewModel, AddressAction.Start)

        assertEquals(casa, viewModel.state.value.savedAddress)
    }

    @Test
    fun `a visitor entering is told to sign in`() = runTest(testDispatcher) {
        fixture.users.signedIn = null
        val viewModel = fixture.addressViewModel()

        send(viewModel, AddressAction.Start)

        assertEquals(DomainError.Unauthorized, viewModel.state.value.loadError)
    }

    @Test
    fun `typing searches and lists the candidates`() = runTest(testDispatcher) {
        val viewModel = fixture.addressViewModel()
        send(viewModel, AddressAction.Start, AddressAction.QueryChanged("  Corr  "))

        assertEquals(listOf(casa, oficina), viewModel.state.value.results)
        assertEquals(listOf("Corr"), fixture.addressSearch.queries)
        assertFalse(viewModel.state.value.isSearching)
    }

    @Test
    fun `a query shorter than three characters does not search`() = runTest(testDispatcher) {
        val viewModel = fixture.addressViewModel()

        send(viewModel, AddressAction.Start, AddressAction.QueryChanged("Co"))

        assertEquals(emptyList(), fixture.addressSearch.queries)
        assertTrue(viewModel.state.value.results.isEmpty())
    }

    @Test
    fun `a failed search is reported and keeps the picker usable`() = runTest(testDispatcher) {
        fixture.addressSearch.failure = DomainError.NoConnection
        val viewModel = fixture.addressViewModel()

        send(viewModel, AddressAction.Start, AddressAction.QueryChanged("Corrientes"))

        assertEquals(DomainError.NoConnection, viewModel.state.value.searchError)
        assertFalse(viewModel.state.value.hasSearched)
    }

    @Test
    fun `saving without choosing a candidate does nothing`() = runTest(testDispatcher) {
        val viewModel = fixture.addressViewModel()

        send(viewModel, AddressAction.Start, AddressAction.SaveClicked)

        assertNull(fixture.users.signedIn?.address)
    }

    /** Escenario 1: "la dirección queda guardada en su perfil". */
    @Test
    fun `picking a result and saving adds the address and finishes`() = runTest(testDispatcher) {
        val viewModel = fixture.addressViewModel()
        send(viewModel, AddressAction.Start, AddressAction.QueryChanged("Corrientes"), AddressAction.ResultSelected(casa))
        assertTrue(viewModel.state.value.canSave)

        val event = sendAndAwaitEvent(viewModel, AddressAction.SaveClicked)

        assertEquals(AddressEvent.Finished, event)
        assertEquals(casa, fixture.users.signedIn?.address)
    }

    /** Escenario 3: the new address replaces the saved one (update, not add). */
    @Test
    fun `with a saved address the save replaces it`() = runTest(testDispatcher) {
        fixture.users.signIn(BookingFixture.CUSTOMER.copy(address = casa))
        val viewModel = fixture.addressViewModel()
        send(viewModel, AddressAction.Start, AddressAction.QueryChanged("Santa Fe"), AddressAction.ResultSelected(oficina))

        sendAndAwaitEvent(viewModel, AddressAction.SaveClicked)

        assertEquals(oficina, fixture.users.signedIn?.address)
    }

    /** Escenario 4: an incomplete address is rejected as a form error and nothing is saved. */
    @Test
    fun `an address without a valid position is a form error and saves nothing`() = runTest(testDispatcher) {
        val broken = Address("Calle sin mapa", 200.0, 0.0)
        val viewModel = fixture.addressViewModel()
        send(viewModel, AddressAction.Start, AddressAction.ResultSelected(broken), AddressAction.SaveClicked)

        val state = viewModel.state.value
        assertEquals("La ubicación de la dirección no es válida", state.saveError)
        assertFalse(state.isSaving)
        assertNull(fixture.users.signedIn?.address)
    }

    @Test
    fun `entering again resets the previous query and selection`() = runTest(testDispatcher) {
        val viewModel = fixture.addressViewModel()
        send(viewModel, AddressAction.Start, AddressAction.QueryChanged("Corrientes"), AddressAction.ResultSelected(casa))

        send(viewModel, AddressAction.Start)

        val state = viewModel.state.value
        assertEquals("", state.query)
        assertNull(state.selected)
        assertTrue(state.results.isEmpty())
    }

    @Test
    fun `entering again shows the address saved meanwhile as the current one`() = runTest(testDispatcher) {
        val viewModel = fixture.addressViewModel()
        send(viewModel, AddressAction.Start, AddressAction.ResultSelected(casa))
        sendAndAwaitEvent(viewModel, AddressAction.SaveClicked)

        send(viewModel, AddressAction.Start)

        assertEquals(casa, viewModel.state.value.savedAddress)
    }

    @Test
    fun `back finishes without saving`() = runTest(testDispatcher) {
        val viewModel = fixture.addressViewModel()
        send(viewModel, AddressAction.Start, AddressAction.ResultSelected(casa))

        assertEquals(AddressEvent.Finished, sendAndAwaitEvent(viewModel, AddressAction.BackClicked))
        assertNull(fixture.users.signedIn?.address)
    }
}
