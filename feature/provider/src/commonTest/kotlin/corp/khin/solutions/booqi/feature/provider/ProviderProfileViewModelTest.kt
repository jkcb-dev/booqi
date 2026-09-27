@file:OptIn(ExperimentalCoroutinesApi::class)

package corp.khin.solutions.booqi.feature.provider

import corp.khin.solutions.booqi.domain.model.DateRange
import corp.khin.solutions.booqi.domain.usecase.ActivarModoProveedorUseCase
import corp.khin.solutions.booqi.domain.usecase.CompletarPerfilDeProveedorUseCase
import corp.khin.solutions.booqi.domain.usecase.PausarPerfilUseCase
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
import kotlinx.datetime.LocalDate
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * One test per Gherkin scenario in docs/domain/provider-flow.md § Grupo 1, exercised through
 * [ProviderProfileViewModel]'s reducer — same BDD scenarios `domain`'s use-case tests cover
 * (issue #12), verified here at the presentation layer (state/events), not just the use case
 * return value.
 */
class ProviderProfileViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun newViewModel(repository: FakeProviderProfileRepository = FakeProviderProfileRepository()) =
        ProviderProfileViewModel(
            activarModoProveedor = ActivarModoProveedorUseCase(repository),
            completarPerfil = CompletarPerfilDeProveedorUseCase(repository),
            pausarPerfil = PausarPerfilUseCase(repository),
        )

    /** Dispatches [action] and suspends until the single event it emits is collected. A fresh
     * collector per call means no event is ever left stuck, unread, in the (rendezvous) events
     * Channel for a later call to pick up by accident. */
    private suspend fun TestScope.performAndAwaitEvent(
        viewModel: ProviderProfileViewModel,
        action: ProviderProfileAction,
    ): ProviderProfileEvent {
        var event: ProviderProfileEvent? = null
        val collectorJob = launch { event = viewModel.events.first() }
        viewModel.onAction(action)
        advanceUntilIdle()
        collectorJob.join()
        return checkNotNull(event) { "No event received for $action" }
    }

    private fun completeProfileFields(viewModel: ProviderProfileViewModel) {
        viewModel.onAction(ProviderProfileAction.NameChanged("Ana Pérez"))
        viewModel.onAction(ProviderProfileAction.PhotoUrlChanged("https://example.com/ana.jpg"))
        viewModel.onAction(ProviderProfileAction.DescriptionChanged("Estilista con 5 años de experiencia"))
        viewModel.onAction(ProviderProfileAction.LocationChanged("Ciudad de Panamá"))
    }

    /** Escenario: "Un usuario activa el modo Proveedor". */
    @Test
    fun `activating provider mode creates an empty profile and exposes it in state`() = runTest(testDispatcher) {
        val viewModel = newViewModel()

        viewModel.onAction(ProviderProfileAction.ActivateProviderMode)
        advanceUntilIdle()

        val profile = viewModel.state.value.profile
        assertTrue(profile != null)
        assertEquals(false, profile.isComplete)
        assertNull(profile.name)
        assertNull(profile.location)
    }

    /** Escenario: "El Proveedor completa su perfil". */
    @Test
    fun `saving the profile with all fields marks it complete and emits ProfileSaved`() = runTest(testDispatcher) {
        val viewModel = newViewModel()
        viewModel.onAction(ProviderProfileAction.ActivateProviderMode)
        advanceUntilIdle()
        completeProfileFields(viewModel)

        val event = performAndAwaitEvent(viewModel, ProviderProfileAction.SaveProfile)

        assertEquals(ProviderProfileEvent.ProfileSaved, event)
        val profile = viewModel.state.value.profile
        assertTrue(profile != null)
        assertEquals(true, profile.isComplete)
        assertEquals("Ana Pérez", profile.name)
        assertNull(viewModel.state.value.locationError)
    }

    /** Escenario: "El Proveedor intenta completar el perfil sin ubicación". */
    @Test
    fun `saving without a location rejects the save and surfaces a form error`() = runTest(testDispatcher) {
        val viewModel = newViewModel()
        viewModel.onAction(ProviderProfileAction.ActivateProviderMode)
        advanceUntilIdle()

        viewModel.onAction(ProviderProfileAction.NameChanged("Ana Pérez"))
        viewModel.onAction(ProviderProfileAction.DescriptionChanged("Estilista"))
        // locationInput left blank on purpose.

        viewModel.onAction(ProviderProfileAction.SaveProfile)
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(false, state.profile?.isComplete)
        assertEquals("La ubicación es obligatoria", state.locationError)
    }

    /** Escenario: "El Proveedor pausa su perfil por un rango de fechas". */
    @Test
    fun `confirming a pause range hides the profile and does not touch confirmed bookings`() = runTest(testDispatcher) {
        val viewModel = newViewModel()
        viewModel.onAction(ProviderProfileAction.ActivateProviderMode)
        advanceUntilIdle()
        completeProfileFields(viewModel)
        performAndAwaitEvent(viewModel, ProviderProfileAction.SaveProfile)

        viewModel.onAction(ProviderProfileAction.ShowPauseSheet)
        viewModel.onAction(ProviderProfileAction.PauseFromChanged(LocalDate(2026, 8, 10)))
        viewModel.onAction(ProviderProfileAction.PauseUntilChanged(LocalDate(2026, 8, 20)))
        val event = performAndAwaitEvent(viewModel, ProviderProfileAction.ConfirmPause)

        assertEquals(ProviderProfileEvent.ProfilePaused, event)
        val profile = viewModel.state.value.profile
        assertTrue(profile != null)
        assertEquals(true, profile.isPaused)
        assertEquals(DateRange(LocalDate(2026, 8, 10), LocalDate(2026, 8, 20)), profile.pausedRange)
        assertEquals(false, viewModel.state.value.isPauseSheetVisible)
    }

    /** Escenario: "El Proveedor reactiva su perfil antes de tiempo". */
    @Test
    fun `reactivating a paused profile clears the paused range immediately`() = runTest(testDispatcher) {
        val viewModel = newViewModel()
        viewModel.onAction(ProviderProfileAction.ActivateProviderMode)
        advanceUntilIdle()
        completeProfileFields(viewModel)
        performAndAwaitEvent(viewModel, ProviderProfileAction.SaveProfile)
        viewModel.onAction(ProviderProfileAction.ShowPauseSheet)
        viewModel.onAction(ProviderProfileAction.PauseFromChanged(LocalDate(2026, 8, 10)))
        viewModel.onAction(ProviderProfileAction.PauseUntilChanged(LocalDate(2026, 8, 20)))
        performAndAwaitEvent(viewModel, ProviderProfileAction.ConfirmPause)
        assertTrue(viewModel.state.value.profile?.isPaused == true)

        val event = performAndAwaitEvent(viewModel, ProviderProfileAction.ReactivateProfile)

        assertEquals(ProviderProfileEvent.ProfileReactivated, event)
        val profile = viewModel.state.value.profile
        assertTrue(profile != null)
        assertEquals(false, profile.isPaused)
        assertNull(profile.pausedRange)
    }
}
