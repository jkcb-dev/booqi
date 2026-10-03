@file:OptIn(ExperimentalCoroutinesApi::class)

package corp.khin.solutions.booqi.feature.browse

import corp.khin.solutions.booqi.core.common.DomainError
import corp.khin.solutions.booqi.domain.model.ServiceCategory
import corp.khin.solutions.booqi.domain.usecase.BuscarServiciosUseCase
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
import kotlinx.datetime.TimeZone
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Grupo 1 scenarios (docs/domain/customer-flow.md) that surface on C1/C2, exercised through
 * [BrowseViewModel]'s reducer — actions in, state and events out — over the real
 * [BuscarServiciosUseCase] and a fake catalog. Results come back nearest first from the TEMPORARY
 * customer location (Studio Booqi ~1.3 km, Casa Brillante ~4.4 km), then by title.
 */
class BrowseViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val catalog = FakeCatalogRepository(CatalogFixture.entries)

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun newViewModel() = BrowseViewModel(BuscarServiciosUseCase(catalog, FixedClock, TimeZone.UTC))

    private fun TestScope.send(viewModel: BrowseViewModel, vararg actions: BrowseAction) {
        actions.forEach {
            viewModel.onAction(it)
            advanceUntilIdle()
        }
    }

    private fun TestScope.search(viewModel: BrowseViewModel, text: String) =
        send(viewModel, BrowseAction.QueryChanged(text), BrowseAction.SearchSubmitted)

    private fun BrowseViewModel.titles(): List<String> = state.value.results.map { it.service.title }

    @Test
    fun `starts on C1 without searching`() = runTest(testDispatcher) {
        val viewModel = newViewModel()
        advanceUntilIdle()

        assertFalse(viewModel.state.value.hasSearched)
        assertTrue(viewModel.state.value.results.isEmpty())
        assertTrue(viewModel.state.value.recentSearches.isEmpty())
        assertEquals(0, catalog.calls)
    }

    @Test
    fun `refresh before the first search does nothing`() = runTest(testDispatcher) {
        val viewModel = newViewModel()

        send(viewModel, BrowseAction.Refresh)

        assertFalse(viewModel.state.value.hasSearched)
        assertEquals(0, catalog.calls)
    }

    /** Escenario: "El Cliente busca un Servicio por texto" — title or description, accents ignored. */
    @Test
    fun `text search matches title or description ignoring case and accents`() = runTest(testDispatcher) {
        val viewModel = newViewModel()

        search(viewModel, "UNAS")

        assertTrue(viewModel.state.value.hasSearched)
        assertEquals(listOf("Corte de uñas", "Uñas esculpidas"), viewModel.titles())
        assertEquals(2, viewModel.state.value.resultCount)
        assertNull(viewModel.state.value.error)
    }

    @Test
    fun `typing alone does not search`() = runTest(testDispatcher) {
        val viewModel = newViewModel()

        send(viewModel, BrowseAction.QueryChanged("corte"))

        assertEquals("corte", viewModel.state.value.query)
        assertFalse(viewModel.state.value.hasSearched)
        assertEquals(0, catalog.calls)
    }

    /** Escenario: "El Cliente busca por categoría (chip)". */
    @Test
    fun `category chip shows only that category`() = runTest(testDispatcher) {
        val viewModel = newViewModel()

        send(viewModel, BrowseAction.CategorySelected(ServiceCategory.BARBERIA))

        assertEquals(listOf("Corte y barba"), viewModel.titles())
        assertEquals(ServiceCategory.BARBERIA, viewModel.state.value.category)
    }

    @Test
    fun `todos chip shows every service again`() = runTest(testDispatcher) {
        val viewModel = newViewModel()

        send(viewModel, BrowseAction.CategorySelected(ServiceCategory.BARBERIA), BrowseAction.CategorySelected(null))

        assertEquals(CatalogFixture.services.size, viewModel.state.value.resultCount)
        assertNull(viewModel.state.value.category)
    }

    /** Escenario: "Texto y categoría se combinan". */
    @Test
    fun `text and category chip combine`() = runTest(testDispatcher) {
        val viewModel = newViewModel()

        search(viewModel, "corte")
        assertEquals(listOf("Corte de uñas", "Corte y barba"), viewModel.titles())

        send(viewModel, BrowseAction.CategorySelected(ServiceCategory.BARBERIA))
        assertEquals(listOf("Corte y barba"), viewModel.titles())
    }

    /** Escenario: "El Cliente filtra por distancia" — 1/2/5/10 km around the TEMPORARY location. */
    @Test
    fun `distance filter limits results to the radius`() = runTest(testDispatcher) {
        val viewModel = newViewModel()
        send(viewModel, BrowseAction.CategorySelected(null))
        assertEquals(6, viewModel.state.value.resultCount)

        send(viewModel, BrowseAction.DistanceSelected(1))
        assertEquals(0, viewModel.state.value.resultCount)

        send(viewModel, BrowseAction.DistanceSelected(2))
        assertEquals(
            listOf("Corte de uñas", "Corte y barba", "Masaje descontracturante"),
            viewModel.titles(),
        )

        send(viewModel, BrowseAction.DistanceSelected(5))
        assertEquals(6, viewModel.state.value.resultCount)

        send(viewModel, BrowseAction.DistanceSelected(10))
        assertEquals(6, viewModel.state.value.resultCount)

        send(viewModel, BrowseAction.DistanceSelected(null))
        assertEquals(6, viewModel.state.value.resultCount)
        assertNull(viewModel.state.value.radiusKm)
    }

    @Test
    fun `distance filter keeps the text and the chip`() = runTest(testDispatcher) {
        val viewModel = newViewModel()
        search(viewModel, "corte")

        send(viewModel, BrowseAction.DistanceSelected(2), BrowseAction.CategorySelected(ServiceCategory.UNAS))

        assertEquals(listOf("Corte de uñas"), viewModel.titles())
        assertEquals("corte", viewModel.state.value.query)
        assertEquals(2, viewModel.state.value.radiusKm)
    }

    @Test
    fun `every result carries its distance from the temporary location`() = runTest(testDispatcher) {
        val viewModel = newViewModel()

        send(viewModel, BrowseAction.CategorySelected(null))

        val distances = viewModel.state.value.results.associate { it.service.id to checkNotNull(it.distanceKm) }
        assertTrue(distances.getValue("s1") in 1.0..2.0)
        assertTrue(distances.getValue("s3") in 4.0..5.0)
    }

    /** Escenario: "Sin coincidencias no es un error". */
    @Test
    fun `no results is an empty list and not an error`() = runTest(testDispatcher) {
        val viewModel = newViewModel()

        search(viewModel, "yoga")

        assertTrue(viewModel.state.value.hasSearched)
        assertEquals(0, viewModel.state.value.resultCount)
        assertNull(viewModel.state.value.error)
        assertFalse(viewModel.state.value.isLoading)
    }

    @Test
    fun `a failing search shows the error and refresh recovers`() = runTest(testDispatcher) {
        catalog.failure = DomainError.NoConnection
        val viewModel = newViewModel()

        search(viewModel, "corte")
        assertEquals(DomainError.NoConnection, viewModel.state.value.error)
        assertTrue(viewModel.state.value.results.isEmpty())
        assertFalse(viewModel.state.value.isLoading)

        catalog.failure = null
        send(viewModel, BrowseAction.Refresh)
        assertNull(viewModel.state.value.error)
        assertEquals(2, viewModel.state.value.resultCount)
    }

    /** Re-entry: the screen sends Refresh on every entry, so a list changed meanwhile is current. */
    @Test
    fun `refresh re-runs the current search after coming back`() = runTest(testDispatcher) {
        val viewModel = newViewModel()
        search(viewModel, "corte")
        assertEquals(2, viewModel.state.value.resultCount)

        catalog.entries = CatalogFixture.entries.filterNot { it.service.id == "s6" }
        send(viewModel, BrowseAction.Refresh)

        assertEquals(listOf("Corte y barba"), viewModel.titles())
        assertEquals("corte", viewModel.state.value.query)
    }

    @Test
    fun `clear search returns to C1 but keeps the recent searches`() = runTest(testDispatcher) {
        val viewModel = newViewModel()
        search(viewModel, "corte")
        send(viewModel, BrowseAction.DistanceSelected(2), BrowseAction.CategorySelected(ServiceCategory.UNAS))

        send(viewModel, BrowseAction.ClearSearch)

        val state = viewModel.state.value
        assertFalse(state.hasSearched)
        assertEquals("", state.query)
        assertNull(state.category)
        assertNull(state.radiusKm)
        assertTrue(state.results.isEmpty())
        assertEquals(listOf("corte"), state.recentSearches)
    }

    @Test
    fun `recent searches keep submitted texts newest first without duplicates`() = runTest(testDispatcher) {
        val viewModel = newViewModel()

        search(viewModel, "corte")
        search(viewModel, "uñas")
        search(viewModel, "  CORTE ")

        assertEquals(listOf("CORTE", "uñas"), viewModel.state.value.recentSearches)
    }

    @Test
    fun `recent searches keep only the latest five`() = runTest(testDispatcher) {
        val viewModel = newViewModel()

        listOf("uno", "dos", "tres", "cuatro", "cinco", "seis").forEach { search(viewModel, it) }

        assertEquals(listOf("seis", "cinco", "cuatro", "tres", "dos"), viewModel.state.value.recentSearches)
    }

    @Test
    fun `blank text and chip taps are not recent searches`() = runTest(testDispatcher) {
        val viewModel = newViewModel()

        search(viewModel, "   ")
        send(viewModel, BrowseAction.CategorySelected(ServiceCategory.BARBERIA), BrowseAction.DistanceSelected(5))

        assertTrue(viewModel.state.value.recentSearches.isEmpty())
    }

    @Test
    fun `selecting a recent search searches it again and moves it to the top`() = runTest(testDispatcher) {
        val viewModel = newViewModel()
        search(viewModel, "uñas")
        search(viewModel, "corte")

        send(viewModel, BrowseAction.RecentSelected("uñas"))

        assertEquals("uñas", viewModel.state.value.query)
        assertEquals(listOf("Corte de uñas", "Uñas esculpidas"), viewModel.titles())
        assertEquals(listOf("uñas", "corte"), viewModel.state.value.recentSearches)
    }

    @Test
    fun `selecting a service emits the navigation event`() = runTest(testDispatcher) {
        val viewModel = newViewModel()
        var event: BrowseEvent? = null
        val collector = launch { event = viewModel.events.first() }

        viewModel.onAction(BrowseAction.ServiceSelected("s1"))
        advanceUntilIdle()
        collector.join()

        assertEquals(BrowseEvent.NavigateToServiceDetail("s1"), event)
    }
}
