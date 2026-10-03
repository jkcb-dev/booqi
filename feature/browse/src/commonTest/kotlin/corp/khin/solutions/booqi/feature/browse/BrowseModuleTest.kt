@file:OptIn(ExperimentalCoroutinesApi::class)

package corp.khin.solutions.booqi.feature.browse

import corp.khin.solutions.booqi.domain.di.domainModule
import corp.khin.solutions.booqi.domain.repository.BookingRepository
import corp.khin.solutions.booqi.domain.repository.CatalogRepository
import corp.khin.solutions.booqi.domain.repository.ProviderProfileRepository
import corp.khin.solutions.booqi.domain.repository.ServiceRepository
import corp.khin.solutions.booqi.feature.browse.di.browseModule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

/** Proves `browseModule` resolves all three ViewModels against `domainModule`'s use cases. */
class BrowseModuleTest {

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
            browseModule,
            module {
                single<CatalogRepository> { FakeCatalogRepository() }
                single<ServiceRepository> { FakeServiceRepository() }
                single<ProviderProfileRepository> { FakeProviderProfileRepository() }
                single<BookingRepository> { FakeBookingRepository() }
            },
        )
    }.koin

    @Test
    fun `search view model resolves with the temporary location`() {
        koin.get<BrowseViewModel>()
        assertEquals(false, koin.get<BrowseViewModel>().state.value.hasSearched)
    }

    @Test
    fun `detail view model resolves`() {
        koin.get<ServiceDetailViewModel>()
    }

    @Test
    fun `provider profile view model resolves`() {
        koin.get<ProviderPublicProfileViewModel>()
    }
}
