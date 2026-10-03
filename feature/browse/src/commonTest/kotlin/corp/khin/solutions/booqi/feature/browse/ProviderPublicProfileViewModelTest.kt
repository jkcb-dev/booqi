@file:OptIn(ExperimentalCoroutinesApi::class)

package corp.khin.solutions.booqi.feature.browse

import corp.khin.solutions.booqi.core.common.DomainError
import corp.khin.solutions.booqi.domain.model.ServiceCategory
import corp.khin.solutions.booqi.domain.usecase.ObtenerCalificacionesDelProveedorUseCase
import corp.khin.solutions.booqi.domain.usecase.VerPerfilProveedorUseCase
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
 * Escenario "El Cliente ve el perfil completo de un Proveedor" (docs/domain/customer-flow.md
 * § Grupo 1) on C4, through [ProviderPublicProfileViewModel]'s reducer over the real
 * [VerPerfilProveedorUseCase].
 */
class ProviderPublicProfileViewModelTest {

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
    private val bookings = FakeBookingRepository(
        listOf(
            CatalogFixture.ratedBooking("b-old", CatalogFixture.STUDIO_ID, 4, "Muy bueno", "2026-09-10T10:00:00Z"),
            CatalogFixture.ratedBooking("b-new", CatalogFixture.STUDIO_ID, 5, null, "2026-09-20T10:00:00Z"),
            CatalogFixture.ratedBooking("b-other", CatalogFixture.CASA_ID, 3, "Regular", "2026-09-15T10:00:00Z"),
        ),
    )

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun newViewModel() = ProviderPublicProfileViewModel(
        VerPerfilProveedorUseCase(profiles, services, ObtenerCalificacionesDelProveedorUseCase(bookings)),
    )

    private fun TestScope.start(viewModel: ProviderPublicProfileViewModel, providerId: String) {
        viewModel.onAction(ProviderPublicProfileAction.Start(providerId))
        advanceUntilIdle()
    }

    private suspend fun TestScope.performAndAwaitEvent(
        viewModel: ProviderPublicProfileViewModel,
        action: ProviderPublicProfileAction,
    ): ProviderPublicProfileEvent {
        var event: ProviderPublicProfileEvent? = null
        val collector = launch { event = viewModel.events.first() }
        viewModel.onAction(action)
        advanceUntilIdle()
        collector.join()
        return checkNotNull(event) { "No event received for $action" }
    }

    @Test
    fun `start loads the bio location rating active services and reviews`() = runTest(testDispatcher) {
        val viewModel = newViewModel()

        start(viewModel, CatalogFixture.STUDIO_ID)

        val profile = checkNotNull(viewModel.state.value.profile)
        assertEquals(false, viewModel.state.value.isLoading)
        assertEquals("Studio Booqi", profile.provider.name)
        assertEquals("Av. Corrientes 1234, CABA", profile.provider.location)
        assertEquals("Atención profesional con reserva previa.", profile.provider.description)
        assertEquals(4.5, profile.provider.ratingAverage)
        assertEquals(2, profile.provider.ratingCount)
        assertEquals(listOf("s1", "s2", "s6"), profile.services.map { it.id })
        assertEquals(listOf("b-new", "b-old"), profile.reviews.map { it.bookingId })
        assertNull(viewModel.state.value.error)
    }

    @Test
    fun `disabled services are left out of the servicios section`() = runTest(testDispatcher) {
        val viewModel = newViewModel()

        start(viewModel, CatalogFixture.STUDIO_ID)

        assertTrue(viewModel.state.value.profile?.services.orEmpty().none { it.id == "s-off" })
    }

    @Test
    fun `a provider without active services or reviews yields empty lists`() = runTest(testDispatcher) {
        val viewModel = ProviderPublicProfileViewModel(
            VerPerfilProveedorUseCase(
                profiles,
                FakeServiceRepository(),
                ObtenerCalificacionesDelProveedorUseCase(FakeBookingRepository()),
            ),
        )

        start(viewModel, CatalogFixture.CASA_ID)

        val profile = checkNotNull(viewModel.state.value.profile)
        assertTrue(profile.services.isEmpty())
        assertTrue(profile.reviews.isEmpty())
        assertNull(viewModel.state.value.error)
    }

    @Test
    fun `an unknown or incomplete provider is not found`() = runTest(testDispatcher) {
        val viewModel = newViewModel()

        start(viewModel, "nope")
        assertEquals(DomainError.NotFound, viewModel.state.value.error)
        assertNull(viewModel.state.value.profile)

        start(viewModel, "p-incomplete")
        assertEquals(DomainError.NotFound, viewModel.state.value.error)
    }

    /** Re-entry: the ViewModel is shared, so Start for another Provider must wipe the previous one. */
    @Test
    fun `starting another provider replaces the previous one`() = runTest(testDispatcher) {
        val viewModel = newViewModel()
        start(viewModel, CatalogFixture.STUDIO_ID)

        viewModel.onAction(ProviderPublicProfileAction.Start(CatalogFixture.CASA_ID))
        assertNull(viewModel.state.value.profile)
        assertTrue(viewModel.state.value.isLoading)

        advanceUntilIdle()
        assertEquals("Casa Brillante", viewModel.state.value.profile?.provider?.name)
        assertEquals(listOf("b-other"), viewModel.state.value.profile?.reviews?.map { it.bookingId })
    }

    @Test
    fun `retry reloads after a failure`() = runTest(testDispatcher) {
        services.failure = DomainError.Timeout
        val viewModel = newViewModel()
        start(viewModel, CatalogFixture.STUDIO_ID)
        assertEquals(DomainError.Timeout, viewModel.state.value.error)

        services.failure = null
        viewModel.onAction(ProviderPublicProfileAction.Retry)
        advanceUntilIdle()

        assertNull(viewModel.state.value.error)
        assertEquals(3, viewModel.state.value.profile?.services?.size)
    }

    @Test
    fun `a service card opens its detail`() = runTest(testDispatcher) {
        val viewModel = newViewModel()
        start(viewModel, CatalogFixture.STUDIO_ID)

        val event = performAndAwaitEvent(viewModel, ProviderPublicProfileAction.ServiceClicked("s2"))

        assertEquals(ProviderPublicProfileEvent.NavigateToService("s2"), event)
    }

    @Test
    fun `reservar on a card hands the service and the provider to the booking flow`() = runTest(testDispatcher) {
        val viewModel = newViewModel()
        start(viewModel, CatalogFixture.STUDIO_ID)

        val event = performAndAwaitEvent(viewModel, ProviderPublicProfileAction.BookClicked("s2"))

        assertEquals(ProviderPublicProfileEvent.NavigateToBooking("s2", CatalogFixture.STUDIO_ID), event)
    }

    @Test
    fun `back asks to leave the screen`() = runTest(testDispatcher) {
        val viewModel = newViewModel()

        val event = performAndAwaitEvent(viewModel, ProviderPublicProfileAction.BackClicked)

        assertEquals(ProviderPublicProfileEvent.NavigateBack, event)
    }
}
