@file:OptIn(ExperimentalCoroutinesApi::class)

package corp.khin.solutions.booqi.feature.account

import corp.khin.solutions.booqi.domain.model.SocialProvider
import corp.khin.solutions.booqi.domain.model.User
import corp.khin.solutions.booqi.domain.usecase.IniciarSesionUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * Sign-in (docs/domain/identity-flow.md: `IniciarSesion` — email + password, and Google / Facebook /
 * Apple) through [SignInViewModel]'s reducer: state and events out, for actions in.
 */
class SignInViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val users = FakeUserRepository().apply {
        seed(User(id = "u1", displayName = "Ana Pérez", email = "ana@booqi.test", isEmailVerified = true), "secreto123")
    }

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun newViewModel() = SignInViewModel(IniciarSesionUseCase(users)).also { it.onAction(SignInAction.Start) }

    private fun SignInViewModel.fill(email: String, password: String) {
        onAction(SignInAction.EmailChanged(email))
        onAction(SignInAction.PasswordChanged(password))
    }

    @Test
    fun `signing in with the right credentials starts the session and finishes`() = runTest(testDispatcher) {
        val viewModel = newViewModel()
        viewModel.fill("  Ana@Booqi.test ", "secreto123")

        val event = eventAfter(viewModel.events) { viewModel.onAction(SignInAction.SignInClicked) }

        assertEquals(SignInEvent.Authenticated(needsEmailVerification = false), event)
        assertEquals("u1", users.current?.id)
        assertFalse(viewModel.state.value.isSubmitting)
        assertEquals("", viewModel.state.value.password)
    }

    @Test
    fun `wrong credentials are a message on the form and no session`() = runTest(testDispatcher) {
        val viewModel = newViewModel()
        viewModel.fill("ana@booqi.test", "incorrecta")

        viewModel.onAction(SignInAction.SignInClicked)
        advanceUntilIdle()

        assertEquals("Correo o contraseña incorrectos", viewModel.state.value.formError)
        assertNull(viewModel.state.value.emailError)
        assertNull(users.current)
        assertFalse(viewModel.state.value.isSubmitting)
    }

    @Test
    fun `an empty email is an error on the email field`() = runTest(testDispatcher) {
        val viewModel = newViewModel()
        viewModel.fill("", "secreto123")

        viewModel.onAction(SignInAction.SignInClicked)
        advanceUntilIdle()

        assertEquals("El correo es obligatorio", viewModel.state.value.emailError)
        assertNull(viewModel.state.value.formError)
    }

    @Test
    fun `an empty password is an error on the password field`() = runTest(testDispatcher) {
        val viewModel = newViewModel()
        viewModel.fill("ana@booqi.test", "")

        viewModel.onAction(SignInAction.SignInClicked)
        advanceUntilIdle()

        assertEquals("La contraseña es obligatoria", viewModel.state.value.passwordError)
    }

    /** Escenario: "Un visitante se registra con Google, Facebook o Apple" — the sign-in twin. */
    @Test
    fun `a social button signs in with that provider and the email counts as verified`() =
        runTest(testDispatcher) {
            val viewModel = newViewModel()

            val event = eventAfter(viewModel.events) {
                viewModel.onAction(SignInAction.SocialClicked(SocialProvider.GOOGLE))
            }

            assertEquals(SignInEvent.Authenticated(needsEmailVerification = false), event)
            assertEquals(true, users.current?.isEmailVerified)
            assertEquals(SocialProvider.GOOGLE, users.current?.socialProvider)
        }

    @Test
    fun `editing a field clears the errors shown`() = runTest(testDispatcher) {
        val viewModel = newViewModel()
        viewModel.fill("ana@booqi.test", "incorrecta")
        viewModel.onAction(SignInAction.SignInClicked)
        advanceUntilIdle()
        assertNotNull(viewModel.state.value.formError)

        viewModel.onAction(SignInAction.PasswordChanged("otra"))

        assertNull(viewModel.state.value.formError)
    }

    /** The ViewModel outlives a visit (docs/DEVELOPMENT.md): entering again starts on a clean form. */
    @Test
    fun `entering again wipes the previous email password and errors`() = runTest(testDispatcher) {
        val viewModel = newViewModel()
        viewModel.fill("ana@booqi.test", "incorrecta")
        viewModel.onAction(SignInAction.SignInClicked)
        advanceUntilIdle()

        viewModel.onAction(SignInAction.Start)

        assertEquals(SignInUiState(), viewModel.state.value)
    }

    @Test
    fun `the way to sign up and closing are events`() = runTest(testDispatcher) {
        val viewModel = newViewModel()

        assertEquals(
            SignInEvent.SwitchToSignUp,
            eventAfter(viewModel.events) { viewModel.onAction(SignInAction.SwitchToSignUpClicked) },
        )
        assertEquals(
            SignInEvent.Close,
            eventAfter(viewModel.events) { viewModel.onAction(SignInAction.CloseClicked) },
        )
    }
}
