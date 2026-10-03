@file:OptIn(ExperimentalCoroutinesApi::class)

package corp.khin.solutions.booqi.feature.browse

import corp.khin.solutions.booqi.core.common.DomainError
import corp.khin.solutions.booqi.domain.model.ServiceCategory
import corp.khin.solutions.booqi.domain.usecase.VerDetalleServicioUseCase
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
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Escenario "El Cliente ve el detalle de un Servicio" (docs/domain/customer-flow.md § Grupo 1) on
 * C3, through [ServiceDetailViewModel]'s reducer over the real [VerDetalleServicioUseCase].
 */
class ServiceDetailViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val disabled = CatalogFixture.service(
        id = "s-off",
        providerId = CatalogFixture.STUDIO_ID,
        title = "Deshabilitado",
        category = ServiceCategory.OTRO,
        isActive = false,
    )
    private val services = FakeServiceRepository(CatalogFixture.services + disabled)
    private val incompleteProfile = CatalogFixture.studio.copy(id = "p-incomplete", isComplete = false)
    private val profiles = FakeProviderProfileRepository(
        listOf(CatalogFixture.studio, CatalogFixture.casa, incompleteProfile),
    )

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun newViewModel() = ServiceDetailViewModel(VerDetalleServicioUseCase(services, profiles))

    private fun TestScope.start(viewModel: ServiceDetailViewModel, serviceId: String) {
        viewModel.onAction(ServiceDetailAction.Start(serviceId))
        advanceUntilIdle()
    }

    private suspend fun TestScope.performAndAwaitEvent(
        viewModel: ServiceDetailViewModel,
        action: ServiceDetailAction,
    ): ServiceDetailEvent {
        var event: ServiceDetailEvent? = null
        val collector = launch { event = viewModel.events.first() }
        viewModel.onAction(action)
        advanceUntilIdle()
        collector.join()
        return checkNotNull(event) { "No event received for $action" }
    }

    @Test
    fun `start loads the service with its provider`() = runTest(testDispatcher) {
        val viewModel = newViewModel()

        start(viewModel, "s1")

        val state = viewModel.state.value
        assertEquals(false, state.isLoading)
        assertEquals("s1", state.serviceId)
        assertEquals(CatalogFixture.corteYBarba, state.detail?.service)
        assertEquals("Studio Booqi", state.detail?.provider?.name)
        assertEquals(4.5, state.detail?.provider?.ratingAverage)
        assertNull(state.error)
    }

    @Test
    fun `start shows loading until the service arrives`() = runTest(testDispatcher) {
        val viewModel = newViewModel()

        viewModel.onAction(ServiceDetailAction.Start("s1"))

        assertTrue(viewModel.state.value.isLoading)
        assertNull(viewModel.state.value.detail)
    }

    @Test
    fun `an unknown service is not found`() = runTest(testDispatcher) {
        val viewModel = newViewModel()

        start(viewModel, "nope")

        assertEquals(DomainError.NotFound, viewModel.state.value.error)
        assertNull(viewModel.state.value.detail)
        assertEquals(false, viewModel.state.value.isLoading)
    }

    @Test
    fun `a disabled service is not found`() = runTest(testDispatcher) {
        val viewModel = newViewModel()

        start(viewModel, "s-off")

        assertEquals(DomainError.NotFound, viewModel.state.value.error)
    }

    @Test
    fun `a service of an incomplete profile is not found`() = runTest(testDispatcher) {
        val orphan = FakeServiceRepository(
            listOf(CatalogFixture.service("s-x", "p-incomplete", "X", ServiceCategory.OTRO)),
        )
        val viewModel = ServiceDetailViewModel(VerDetalleServicioUseCase(orphan, profiles))

        start(viewModel, "s-x")

        assertEquals(DomainError.NotFound, viewModel.state.value.error)
    }

    /** Re-entry: the ViewModel is shared, so Start for another Service must wipe the previous one. */
    @Test
    fun `starting another service replaces the previous one`() = runTest(testDispatcher) {
        val viewModel = newViewModel()
        start(viewModel, "s1")

        viewModel.onAction(ServiceDetailAction.Start("s3"))
        assertNull(viewModel.state.value.detail)
        assertTrue(viewModel.state.value.isLoading)
        assertEquals("s3", viewModel.state.value.serviceId)

        advanceUntilIdle()
        assertEquals(CatalogFixture.limpieza, viewModel.state.value.detail?.service)
        assertEquals("Casa Brillante", viewModel.state.value.detail?.provider?.name)
    }

    @Test
    fun `retry reloads after a failure`() = runTest(testDispatcher) {
        services.failure = DomainError.NoConnection
        val viewModel = newViewModel()
        start(viewModel, "s1")
        assertEquals(DomainError.NoConnection, viewModel.state.value.error)

        services.failure = null
        viewModel.onAction(ServiceDetailAction.Retry)
        advanceUntilIdle()

        assertNull(viewModel.state.value.error)
        assertEquals("s1", viewModel.state.value.detail?.service?.id)
    }

    @Test
    fun `provider click opens the provider profile`() = runTest(testDispatcher) {
        val viewModel = newViewModel()
        start(viewModel, "s3")

        val event = performAndAwaitEvent(viewModel, ServiceDetailAction.ProviderClicked)

        assertEquals(ServiceDetailEvent.NavigateToProvider(CatalogFixture.CASA_ID), event)
    }

    @Test
    fun `reservar hands the service and its provider to the booking flow`() = runTest(testDispatcher) {
        val viewModel = newViewModel()
        start(viewModel, "s3")

        val event = performAndAwaitEvent(viewModel, ServiceDetailAction.BookClicked)

        assertEquals(ServiceDetailEvent.NavigateToBooking("s3", CatalogFixture.CASA_ID), event)
    }

    @Test
    fun `back asks to leave the screen`() = runTest(testDispatcher) {
        val viewModel = newViewModel()

        val event = performAndAwaitEvent(viewModel, ServiceDetailAction.BackClicked)

        assertEquals(ServiceDetailEvent.NavigateBack, event)
    }

    @Test
    fun `provider and reservar do nothing before the service has loaded`() = runTest(testDispatcher) {
        val viewModel = newViewModel()
        val collector = launch { viewModel.events.first() }

        viewModel.onAction(ServiceDetailAction.ProviderClicked)
        viewModel.onAction(ServiceDetailAction.BookClicked)
        advanceUntilIdle()

        assertTrue(collector.isActive)
        collector.cancel()
    }
}
