@file:OptIn(ExperimentalCoroutinesApi::class)

package corp.khin.solutions.booqi.feature.account

import corp.khin.solutions.booqi.domain.di.domainModule
import corp.khin.solutions.booqi.domain.repository.BookingRepository
import corp.khin.solutions.booqi.domain.repository.ProviderProfileRepository
import corp.khin.solutions.booqi.domain.repository.ServiceRepository
import corp.khin.solutions.booqi.domain.repository.UserRepository
import corp.khin.solutions.booqi.feature.account.di.accountModule
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

/** Proves `accountModule` resolves every ViewModel against `domainModule`'s use cases. */
class AccountModuleTest {

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
            accountModule,
            module {
                single<UserRepository> { FakeUserRepository() }
                single<BookingRepository> { FakeBookingRepository() }
                single<ProviderProfileRepository> { FakeProviderProfileRepository() }
                single<ServiceRepository> { FakeServiceRepository() }
            },
        )
    }.koin

    @Test
    fun `the sign in and sign up view models resolve with a clean form`() {
        assertTrue(koin.get<SignInViewModel>().state.value == SignInUiState())
        assertTrue(koin.get<SignUpViewModel>().state.value == SignUpUiState())
    }

    @Test
    fun `the verification and profile view models resolve and start loading`() {
        assertTrue(koin.get<VerifyEmailViewModel>().state.value.isLoading)
        assertTrue(koin.get<ProfileViewModel>().state.value.isLoading)
    }

    @Test
    fun `the account deletion view model resolves closed`() {
        assertTrue(!koin.get<DeleteAccountViewModel>().state.value.isOpen)
    }
}
