@file:OptIn(ExperimentalCoroutinesApi::class)

package corp.khin.solutions.booqi.feature.provider

import corp.khin.solutions.booqi.domain.di.domainModule
import corp.khin.solutions.booqi.domain.repository.AvailabilityRepository
import corp.khin.solutions.booqi.domain.repository.ProviderProfileRepository
import corp.khin.solutions.booqi.domain.repository.ServiceRepository
import corp.khin.solutions.booqi.feature.provider.di.providerModule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.koin.core.parameter.parametersOf
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Proves `providerModule` actually resolves the service and schedule ViewModels — in particular the nullable
 * `serviceId` Koin parameter `ServiceEditorScreen` passes with `parametersOf(serviceId)` — since
 * nothing registers the module in the app until Architect's wiring PR.
 */
class ProviderModuleTest {

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private val koin = koinApplication {
        modules(
            domainModule,
            providerModule,
            module {
                single<ServiceRepository> { FakeServiceRepository() }
                single<ProviderProfileRepository> { FakeProviderProfileRepository() }
                single<AvailabilityRepository> { FakeAvailabilityRepository() }
            },
        )
    }.koin

    @Test
    fun `list view model resolves`() {
        koin.get<ServiceListViewModel>()
    }

    @Test
    fun `editor view model resolves in add mode with a null service id`() {
        val viewModel = koin.get<ServiceEditorViewModel> { parametersOf(null) }

        assertFalse(viewModel.state.value.isEditing)
        assertFalse(viewModel.state.value.isLoading)
    }

    @Test
    fun `a ViewModel that Koin hands out again can be re-initialised to another service id`() {
        val viewModel = koin.get<ServiceEditorViewModel> { parametersOf(null) }

        viewModel.onAction(ServiceEditorAction.Start("service-9"))

        assertEquals("service-9", viewModel.state.value.serviceId)
        assertTrue(viewModel.state.value.isLoading)
    }

    @Test
    fun `editor view model resolves in edit mode with a service id`() {
        val viewModel = koin.get<ServiceEditorViewModel> { parametersOf("service-1") }

        assertTrue(viewModel.state.value.isEditing)
        assertEquals("service-1", viewModel.state.value.serviceId)
    }

    @Test
    fun `schedule view models resolve and start loading`() {
        val weekly = koin.get<WeeklyScheduleViewModel>()
        val blocking = koin.get<DateBlockingViewModel>()

        assertTrue(weekly.state.value.isLoading)
        assertTrue(blocking.state.value.isLoading)
    }

    @Test
    fun `profile view model still resolves`() {
        koin.get<ProviderProfileViewModel>()
    }
}
