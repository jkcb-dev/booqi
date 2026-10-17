@file:OptIn(ExperimentalCoroutinesApi::class)

package corp.khin.solutions.booqi.feature.booking

import corp.khin.solutions.booqi.domain.di.domainModule
import corp.khin.solutions.booqi.domain.repository.AvailabilityRepository
import corp.khin.solutions.booqi.domain.repository.BookingRepository
import corp.khin.solutions.booqi.domain.repository.ProviderProfileRepository
import corp.khin.solutions.booqi.domain.repository.ServiceRepository
import corp.khin.solutions.booqi.domain.repository.UserRepository
import corp.khin.solutions.booqi.feature.booking.di.bookingModule
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
import kotlin.test.assertTrue

/** Proves `bookingModule` resolves all four ViewModels against `domainModule`'s use cases. */
class BookingModuleTest {

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
            bookingModule,
            module {
                single<BookingRepository> { FakeBookingRepository() }
                single<UserRepository> { FakeUserRepository() }
                single<AvailabilityRepository> { FakeAvailabilityRepository() }
                single<ProviderProfileRepository> { FakeProviderProfileRepository() }
                single<ServiceRepository> { FakeServiceRepository() }
            },
        )
    }.koin

    @Test
    fun `the request flow view model resolves`() {
        assertTrue(koin.get<BookingFlowViewModel>().state.value.isLoading)
    }

    @Test
    fun `the detail view model resolves`() {
        assertTrue(koin.get<BookingDetailViewModel>().state.value.isLoading)
    }

    @Test
    fun `the my bookings view model resolves`() {
        assertTrue(koin.get<MyBookingsViewModel>().state.value.isLoading)
    }

    @Test
    fun `the rate view model resolves`() {
        assertTrue(koin.get<RateBookingViewModel>().state.value.isLoading)
    }
}
