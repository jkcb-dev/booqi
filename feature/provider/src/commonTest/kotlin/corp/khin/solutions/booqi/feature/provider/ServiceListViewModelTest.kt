@file:OptIn(ExperimentalCoroutinesApi::class)

package corp.khin.solutions.booqi.feature.provider

import corp.khin.solutions.booqi.core.common.DomainError
import corp.khin.solutions.booqi.domain.model.ServiceDetails
import corp.khin.solutions.booqi.domain.model.ServiceModality
import corp.khin.solutions.booqi.domain.usecase.DeshabilitarServicioUseCase
import corp.khin.solutions.booqi.domain.usecase.HabilitarServicioUseCase
import corp.khin.solutions.booqi.domain.usecase.ObtenerServiciosDelProveedorUseCase
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

/**
 * Grupo 2 scenarios (docs/domain/provider-flow.md) that surface on the P4 list, exercised through
 * [ServiceListViewModel]'s reducer: state and events out, for actions in.
 */
class ServiceListViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val repository = FakeServiceRepository()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /** Builds the ViewModel and sends the Refresh [ServiceListScreen] sends on every entry. */
    private fun newViewModel(providerId: String = TEMPORARY_PROVIDER_ID) = ServiceListViewModel(
        obtenerServicios = ObtenerServiciosDelProveedorUseCase(repository),
        deshabilitarServicio = DeshabilitarServicioUseCase(repository),
        habilitarServicio = HabilitarServicioUseCase(repository),
        providerId = providerId,
    ).also { it.onAction(ServiceListAction.Refresh) }

    private fun details(title: String) = ServiceDetails(
        title = title,
        photoUrl = "https://example.com/$title.jpg",
        description = "Descripción de $title",
        priceCents = 1250,
        durationMinutes = 45,
        modality = ServiceModality.LOCAL,
    )

    private suspend fun seed(providerId: String, title: String) =
        repository.addService(providerId, details(title))

    /** Dispatches [action] and suspends until the single event it emits is collected. */
    private suspend fun TestScope.performAndAwaitEvent(
        viewModel: ServiceListViewModel,
        action: ServiceListAction,
    ): ServiceListEvent {
        var event: ServiceListEvent? = null
        val collectorJob = launch { event = viewModel.events.first() }
        viewModel.onAction(action)
        advanceUntilIdle()
        collectorJob.join()
        return checkNotNull(event) { "No event received for $action" }
    }

    /** Escenario: "El Proveedor ve todos sus Servicios, incluidos los deshabilitados". */
    @Test
    fun `list shows own services including disabled ones in creation order and hides other providers`() =
        runTest(testDispatcher) {
            seed(TEMPORARY_PROVIDER_ID, "corte")
            seed("otro-proveedor", "ajeno")
            seed(TEMPORARY_PROVIDER_ID, "barba")
            repository.disableService("service-3")
            val viewModel = newViewModel()

            advanceUntilIdle()

            val state = viewModel.state.value
            assertFalse(state.isLoading)
            assertEquals(listOf("corte", "barba"), state.services.map { it.title })
            assertEquals(listOf(true, false), state.services.map { it.isActive })
            assertNull(state.error)
        }

    @Test
    fun `a provider without services gets an empty list and no error`() = runTest(testDispatcher) {
        val viewModel = newViewModel()

        advanceUntilIdle()

        assertTrue(viewModel.state.value.services.isEmpty())
        assertNull(viewModel.state.value.error)
        assertFalse(viewModel.state.value.isLoading)
    }

    @Test
    fun `a failing load exposes the error and emits ShowError`() = runTest(testDispatcher) {
        repository.listFailure = DomainError.NoConnection
        val viewModel = newViewModel()
        var event: ServiceListEvent? = null
        val collectorJob = launch { event = viewModel.events.first() }

        advanceUntilIdle()
        collectorJob.join()

        assertEquals(DomainError.NoConnection, viewModel.state.value.error)

        assertEquals(ServiceListEvent.ShowError("Sin conexión"), event)
    }

    @Test
    fun `refresh after a failed load recovers`() = runTest(testDispatcher) {
        seed(TEMPORARY_PROVIDER_ID, "corte")
        repository.listFailure = DomainError.NoConnection
        val viewModel = newViewModel()
        advanceUntilIdle() // init load fails; its ShowError send stays suspended (no collector)
        assertEquals(DomainError.NoConnection, viewModel.state.value.error)

        repository.listFailure = null
        viewModel.onAction(ServiceListAction.Refresh)
        advanceUntilIdle()

        assertNull(viewModel.state.value.error)
        assertEquals(listOf("corte"), viewModel.state.value.services.map { it.title })
    }

    /** Escenario: "El Proveedor deshabilita un Servicio". */
    @Test
    fun `disabling a service flips it to inactive in state but keeps it listed`() = runTest(testDispatcher) {
        seed(TEMPORARY_PROVIDER_ID, "corte")
        seed(TEMPORARY_PROVIDER_ID, "barba")
        val viewModel = newViewModel()
        advanceUntilIdle()

        viewModel.onAction(ServiceListAction.SetServiceEnabled("service-1", enabled = false))
        advanceUntilIdle()

        val services = viewModel.state.value.services
        assertEquals(listOf("service-1", "service-2"), services.map { it.id })
        assertEquals(listOf(false, true), services.map { it.isActive })
        assertTrue(viewModel.state.value.togglingServiceIds.isEmpty())
        assertFalse(repository.stored.first { it.id == "service-1" }.isActive)
    }

    /** Escenario: "El Proveedor re-habilita un Servicio deshabilitado". */
    @Test
    fun `re-enabling a disabled service makes it active again and keeps its data`() = runTest(testDispatcher) {
        seed(TEMPORARY_PROVIDER_ID, "corte")
        repository.disableService("service-1")
        val viewModel = newViewModel()
        advanceUntilIdle()
        assertFalse(viewModel.state.value.services.single().isActive)

        viewModel.onAction(ServiceListAction.SetServiceEnabled("service-1", enabled = true))
        advanceUntilIdle()

        val service = viewModel.state.value.services.single()
        assertTrue(service.isActive)
        assertEquals("corte", service.title)
        assertEquals(1250, service.priceCents)
        assertEquals(45, service.durationMinutes)
        assertEquals(ServiceModality.LOCAL, service.modality)
    }

    /** Escenario: "El Proveedor habilita un Servicio que ya estaba activo". */
    @Test
    fun `enabling an already active service is a harmless no-op`() = runTest(testDispatcher) {
        seed(TEMPORARY_PROVIDER_ID, "corte")
        val viewModel = newViewModel()
        advanceUntilIdle()
        val before = viewModel.state.value.services

        viewModel.onAction(ServiceListAction.SetServiceEnabled("service-1", enabled = true))
        advanceUntilIdle()

        assertEquals(before, viewModel.state.value.services)
        assertNull(viewModel.state.value.error)
    }

    /** Escenario: "El Proveedor consulta o habilita un Servicio que no existe". */
    @Test
    fun `toggling an unknown service reports not found and leaves the list untouched`() = runTest(testDispatcher) {
        seed(TEMPORARY_PROVIDER_ID, "corte")
        val viewModel = newViewModel()
        advanceUntilIdle()
        val before = viewModel.state.value.services

        val event = performAndAwaitEvent(
            viewModel,
            ServiceListAction.SetServiceEnabled("no-existe", enabled = true),
        )

        assertEquals(ServiceListEvent.ShowError("No se encontró el servicio"), event)
        assertEquals(DomainError.NotFound, viewModel.state.value.error)
        assertEquals(before, viewModel.state.value.services)
        assertTrue(viewModel.state.value.togglingServiceIds.isEmpty())
        assertEquals(1, repository.stored.size)
    }

    @Test
    fun `add and edit clicks emit navigation events`() = runTest(testDispatcher) {
        seed(TEMPORARY_PROVIDER_ID, "corte")
        val viewModel = newViewModel()
        advanceUntilIdle()

        assertEquals(
            ServiceListEvent.NavigateToAddService,
            performAndAwaitEvent(viewModel, ServiceListAction.AddServiceClicked),
        )
        assertEquals(
            ServiceListEvent.NavigateToEditService("service-1"),
            performAndAwaitEvent(viewModel, ServiceListAction.EditServiceClicked("service-1")),
        )
    }

    /** Regression: the ViewModel instance is reused across visits (navigator isn't destination
     * scoped), so a service added in the editor must show up once the list re-enters. */
    @Test
    fun `a service added elsewhere shows up when the list re-enters and refreshes`() = runTest(testDispatcher) {
        val viewModel = newViewModel()
        advanceUntilIdle()
        assertTrue(viewModel.state.value.services.isEmpty())

        seed(TEMPORARY_PROVIDER_ID, "corte") // what the editor does between two list visits
        viewModel.onAction(ServiceListAction.Refresh) // what ServiceListScreen sends on entry
        advanceUntilIdle()

        assertEquals(listOf("corte"), viewModel.state.value.services.map { it.title })
        assertFalse(viewModel.state.value.isLoading)
    }

    @Test
    fun `a refresh picks up edits made elsewhere to an already listed service`() = runTest(testDispatcher) {
        seed(TEMPORARY_PROVIDER_ID, "corte")
        val viewModel = newViewModel()
        advanceUntilIdle()

        repository.updateService("service-1", details("corte largo").copy(priceCents = 2000))
        viewModel.onAction(ServiceListAction.Refresh)
        advanceUntilIdle()

        val service = viewModel.state.value.services.single()
        assertEquals("corte largo", service.title)
        assertEquals(2000, service.priceCents)
    }

    @Test
    fun `the first frame before the entry refresh is loading and not empty`() {
        val viewModel = ServiceListViewModel(
            obtenerServicios = ObtenerServiciosDelProveedorUseCase(repository),
            deshabilitarServicio = DeshabilitarServicioUseCase(repository),
            habilitarServicio = HabilitarServicioUseCase(repository),
        )

        assertTrue(viewModel.state.value.isLoading)
    }
}
