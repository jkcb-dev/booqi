@file:OptIn(ExperimentalCoroutinesApi::class)

package corp.khin.solutions.booqi.feature.provider

import corp.khin.solutions.booqi.core.common.DomainError
import corp.khin.solutions.booqi.domain.model.ServiceDetails
import corp.khin.solutions.booqi.domain.model.ServiceModality
import corp.khin.solutions.booqi.domain.usecase.AgregarServicioUseCase
import corp.khin.solutions.booqi.domain.usecase.EditarServicioUseCase
import corp.khin.solutions.booqi.domain.usecase.ObtenerServicioUseCase
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
 * Grupo 2 scenarios (docs/domain/provider-flow.md) that surface on the P5 add/edit form,
 * exercised through [ServiceEditorViewModel]'s reducer.
 */
class ServiceEditorViewModelTest {

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

    private fun newViewModel(serviceId: String? = null) = ServiceEditorViewModel(
        serviceId = serviceId,
        agregarServicio = AgregarServicioUseCase(repository),
        editarServicio = EditarServicioUseCase(repository),
        obtenerServicio = ObtenerServicioUseCase(repository),
    )

    /** Dispatches [action] and suspends until the single event it emits is collected. */
    private suspend fun TestScope.performAndAwaitEvent(
        viewModel: ServiceEditorViewModel,
        action: ServiceEditorAction,
    ): ServiceEditorEvent {
        var event: ServiceEditorEvent? = null
        val collectorJob = launch { event = viewModel.events.first() }
        viewModel.onAction(action)
        advanceUntilIdle()
        collectorJob.join()
        return checkNotNull(event) { "No event received for $action" }
    }

    private fun fillForm(
        viewModel: ServiceEditorViewModel,
        photoUrl: String = "https://example.com/corte.jpg",
        price: String = "12.50",
        duration: String = "45",
    ) {
        viewModel.onAction(ServiceEditorAction.PhotoUrlChanged(photoUrl))
        viewModel.onAction(ServiceEditorAction.TitleChanged("Corte clásico"))
        viewModel.onAction(ServiceEditorAction.DescriptionChanged("Corte con tijera y máquina"))
        viewModel.onAction(ServiceEditorAction.PriceChanged(price))
        viewModel.onAction(ServiceEditorAction.DurationChanged(duration))
        viewModel.onAction(ServiceEditorAction.ModalityChanged(ServiceModality.AMBOS))
    }

    private suspend fun seedService(isActive: Boolean = true) {
        repository.addService(
            TEMPORARY_PROVIDER_ID,
            ServiceDetails(
                title = "Corte",
                photoUrl = "https://example.com/original.jpg",
                description = "Original",
                priceCents = 1000,
                durationMinutes = 30,
                modality = ServiceModality.DOMICILIO,
            ),
        )
        if (!isActive) repository.disableService("service-1")
    }

    /** Escenario: "El Proveedor agrega un nuevo Servicio". */
    @Test
    fun `adding a service with all fields saves it for the provider and emits Saved`() = runTest(testDispatcher) {
        val viewModel = newViewModel()
        assertFalse(viewModel.state.value.isEditing)
        assertFalse(viewModel.state.value.isLoading)
        fillForm(viewModel)

        val event = performAndAwaitEvent(viewModel, ServiceEditorAction.Save)

        assertEquals(ServiceEditorEvent.Saved, event)
        val saved = repository.stored.single()
        assertEquals(TEMPORARY_PROVIDER_ID, saved.providerId)
        assertEquals("Corte clásico", saved.title)
        assertEquals("Corte con tijera y máquina", saved.description)
        assertEquals("https://example.com/corte.jpg", saved.photoUrl)
        assertEquals(1250, saved.priceCents)
        assertEquals(45, saved.durationMinutes)
        assertEquals(ServiceModality.AMBOS, saved.modality)
        assertTrue(saved.isActive)
        assertFalse(viewModel.state.value.isSaving)
        assertNull(viewModel.state.value.photoError)
    }

    /** Escenario: "El Proveedor agrega un Servicio sin foto". */
    @Test
    fun `adding a service without a photo shows a photo field error and saves nothing`() = runTest(testDispatcher) {
        val viewModel = newViewModel()
        fillForm(viewModel, photoUrl = "  ")

        viewModel.onAction(ServiceEditorAction.Save)
        advanceUntilIdle()

        assertEquals("La foto es obligatoria", viewModel.state.value.photoError)
        assertNull(viewModel.state.value.error)
        assertFalse(viewModel.state.value.isSaving)
        assertTrue(repository.stored.isEmpty())
    }

    @Test
    fun `the photo error clears when the provider edits the photo and the retry succeeds`() =
        runTest(testDispatcher) {
            val viewModel = newViewModel()
            fillForm(viewModel, photoUrl = "")
            viewModel.onAction(ServiceEditorAction.Save)
            advanceUntilIdle()
            assertEquals("La foto es obligatoria", viewModel.state.value.photoError)

            viewModel.onAction(ServiceEditorAction.PhotoUrlChanged("https://example.com/ok.jpg"))
            assertNull(viewModel.state.value.photoError)
            val event = performAndAwaitEvent(viewModel, ServiceEditorAction.Save)

            assertEquals(ServiceEditorEvent.Saved, event)
            assertEquals(1, repository.stored.size)
        }

    @Test
    fun `a non numeric price or duration is a field error and never reaches the use case`() =
        runTest(testDispatcher) {
            val viewModel = newViewModel()
            fillForm(viewModel, price = "doce", duration = "45 min")

            viewModel.onAction(ServiceEditorAction.Save)
            advanceUntilIdle()

            val state = viewModel.state.value
            assertTrue(state.priceError != null)
            assertTrue(state.durationError != null)
            assertNull(state.photoError)
            assertTrue(repository.stored.isEmpty())

            viewModel.onAction(ServiceEditorAction.PriceChanged("12"))
            assertNull(viewModel.state.value.priceError)
            assertTrue(viewModel.state.value.durationError != null)
        }

    @Test
    fun `an empty form shows price and duration errors instead of crashing`() = runTest(testDispatcher) {
        val viewModel = newViewModel()

        viewModel.onAction(ServiceEditorAction.Save)
        advanceUntilIdle()

        assertTrue(viewModel.state.value.priceError != null)
        assertTrue(viewModel.state.value.durationError != null)
        assertTrue(repository.stored.isEmpty())
    }

    /** Escenario: "El Proveedor consulta un Servicio para editarlo". */
    @Test
    fun `editing preloads every field of the stored service`() = runTest(testDispatcher) {
        seedService(isActive = false)

        val viewModel = newViewModel(serviceId = "service-1")
        assertTrue(viewModel.state.value.isLoading)
        advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse(state.isLoading)
        assertTrue(state.isEditing)
        assertEquals("https://example.com/original.jpg", state.photoUrlInput)
        assertEquals("Corte", state.titleInput)
        assertEquals("Original", state.descriptionInput)
        assertEquals("10.00", state.priceInput)
        assertEquals("30", state.durationInput)
        assertEquals(ServiceModality.DOMICILIO, state.modality)
        assertNull(state.error)
    }

    /** Escenario: "El Proveedor edita un Servicio existente". */
    @Test
    fun `saving an edit updates the stored service without creating another or changing its status`() =
        runTest(testDispatcher) {
            seedService(isActive = false)
            val viewModel = newViewModel(serviceId = "service-1")
            advanceUntilIdle()
            viewModel.onAction(ServiceEditorAction.PriceChanged("15,5"))
            viewModel.onAction(ServiceEditorAction.DurationChanged("60"))
            viewModel.onAction(ServiceEditorAction.ModalityChanged(ServiceModality.LOCAL))

            val event = performAndAwaitEvent(viewModel, ServiceEditorAction.Save)

            assertEquals(ServiceEditorEvent.Saved, event)
            val saved = repository.stored.single()
            assertEquals(1550, saved.priceCents)
            assertEquals(60, saved.durationMinutes)
            assertEquals(ServiceModality.LOCAL, saved.modality)
            assertEquals("Corte", saved.title)
            assertFalse(saved.isActive)
        }

    @Test
    fun `editing a service and clearing its photo is a field error and keeps the stored photo`() =
        runTest(testDispatcher) {
            seedService()
            val viewModel = newViewModel(serviceId = "service-1")
            advanceUntilIdle()
            viewModel.onAction(ServiceEditorAction.PhotoUrlChanged(""))

            viewModel.onAction(ServiceEditorAction.Save)
            advanceUntilIdle()

            assertEquals("La foto es obligatoria", viewModel.state.value.photoError)
            assertEquals("https://example.com/original.jpg", repository.stored.single().photoUrl)
        }

    /** Escenario: "El Proveedor consulta o habilita un Servicio que no existe". */
    @Test
    fun `opening an unknown service reports not found and creates nothing`() = runTest(testDispatcher) {
        val viewModel = newViewModel(serviceId = "no-existe")
        var event: ServiceEditorEvent? = null
        val collectorJob = launch { event = viewModel.events.first() }

        advanceUntilIdle()
        collectorJob.join()

        assertEquals(DomainError.NotFound, viewModel.state.value.error)
        assertFalse(viewModel.state.value.isLoading)
        assertEquals(ServiceEditorEvent.ShowError("No se encontró el servicio"), event)
        assertTrue(repository.stored.isEmpty())
    }
}
