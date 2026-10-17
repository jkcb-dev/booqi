@file:OptIn(ExperimentalCoroutinesApi::class)

package corp.khin.solutions.booqi.feature.account

import corp.khin.solutions.booqi.domain.model.SocialProvider
import corp.khin.solutions.booqi.domain.model.User
import corp.khin.solutions.booqi.domain.usecase.RegistrarUsuarioUseCase
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
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Sign-up scenarios of docs/domain/identity-flow.md ("...con Google, Facebook o Apple", "...con
 * correo y contraseña", "El registro sin nombre se rechaza") through [SignUpViewModel]'s reducer.
 */
class SignUpViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val users = FakeUserRepository()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun newViewModel() = SignUpViewModel(RegistrarUsuarioUseCase(users)).also {
        it.onAction(SignUpAction.Start)
    }

    private fun SignUpViewModel.fill(name: String = "Ana Pérez", email: String = "ana@booqi.test", password: String = "secreto123") {
        onAction(SignUpAction.NameChanged(name))
        onAction(SignUpAction.EmailChanged(email))
        onAction(SignUpAction.PasswordChanged(password))
    }

    /** Escenario: "Un visitante se registra con Google, Facebook o Apple". */
    @Test
    fun `a social sign up creates a verified account with the session started`() = runTest(testDispatcher) {
        val viewModel = newViewModel()

        val event = eventAfter(viewModel.events) {
            viewModel.onAction(SignUpAction.SocialClicked(SocialProvider.APPLE))
        }

        assertEquals(SignUpEvent.Authenticated(needsEmailVerification = false), event)
        assertEquals(true, users.current?.isEmailVerified)
        assertEquals(SocialProvider.APPLE, users.current?.socialProvider)
    }

    /** Escenario: "Un visitante se registra con correo y contraseña". */
    @Test
    fun `an email sign up creates the account signed in and sends the verification email`() =
        runTest(testDispatcher) {
            val viewModel = newViewModel()
            viewModel.fill(name = "  Ana Pérez ", email = " Ana@Booqi.test ")

            val event = eventAfter(viewModel.events) { viewModel.onAction(SignUpAction.SubmitClicked) }

            assertEquals(SignUpEvent.Authenticated(needsEmailVerification = true), event)
            assertEquals("Ana Pérez", users.current?.displayName)
            assertEquals(false, users.current?.isEmailVerified)
            assertEquals(listOf("ana@booqi.test"), users.verificationEmails)
            assertEquals("", viewModel.state.value.password)
        }

    /** Escenario: "El registro sin nombre se rechaza". */
    @Test
    fun `signing up without a name is rejected with the error on the name field`() = runTest(testDispatcher) {
        val viewModel = newViewModel()
        viewModel.fill(name = "   ")

        viewModel.onAction(SignUpAction.SubmitClicked)
        advanceUntilIdle()

        assertEquals("El nombre es obligatorio", viewModel.state.value.nameError)
        assertNull(viewModel.state.value.emailError)
        assertNull(users.current)
        assertTrue(users.verificationEmails.isEmpty())
        assertFalse(viewModel.state.value.isSubmitting)
    }

    @Test
    fun `an invalid email is an error on the email field`() = runTest(testDispatcher) {
        val viewModel = newViewModel()
        viewModel.fill(email = "no-es-un-correo")

        viewModel.onAction(SignUpAction.SubmitClicked)
        advanceUntilIdle()

        assertEquals("El correo no es válido", viewModel.state.value.emailError)
        assertNull(viewModel.state.value.nameError)
    }

    @Test
    fun `a short password is an error on the password field`() = runTest(testDispatcher) {
        val viewModel = newViewModel()
        viewModel.fill(password = "corta")

        viewModel.onAction(SignUpAction.SubmitClicked)
        advanceUntilIdle()

        assertEquals("La contraseña debe tener al menos 8 caracteres", viewModel.state.value.passwordError)
    }

    @Test
    fun `an email that already has an account is an error on the email field`() = runTest(testDispatcher) {
        users.seed(User(id = "u1", displayName = "Otra Ana", email = "ana@booqi.test"), "secreto123")
        val viewModel = newViewModel()
        viewModel.fill()

        viewModel.onAction(SignUpAction.SubmitClicked)
        advanceUntilIdle()

        assertEquals("Ya existe una cuenta con ese correo", viewModel.state.value.emailError)
        assertNull(users.current)
    }

    @Test
    fun `entering again wipes the previous fields and errors`() = runTest(testDispatcher) {
        val viewModel = newViewModel()
        viewModel.fill(name = "")
        viewModel.onAction(SignUpAction.SubmitClicked)
        advanceUntilIdle()

        viewModel.onAction(SignUpAction.Start)

        assertEquals(SignUpUiState(), viewModel.state.value)
    }

    @Test
    fun `editing a field clears its error`() = runTest(testDispatcher) {
        val viewModel = newViewModel()
        viewModel.fill(name = "")
        viewModel.onAction(SignUpAction.SubmitClicked)
        advanceUntilIdle()

        viewModel.onAction(SignUpAction.NameChanged("Ana"))

        assertNull(viewModel.state.value.nameError)
    }

    @Test
    fun `the way to sign in and closing are events`() = runTest(testDispatcher) {
        val viewModel = newViewModel()

        assertEquals(
            SignUpEvent.SwitchToSignIn,
            eventAfter(viewModel.events) { viewModel.onAction(SignUpAction.SwitchToSignInClicked) },
        )
        assertEquals(SignUpEvent.Close, eventAfter(viewModel.events) { viewModel.onAction(SignUpAction.CloseClicked) })
    }

    @Test
    fun `a validation message is attached to the field it is about`() {
        assertEquals(AuthFormField.NAME, fieldOf("El nombre es obligatorio"))
        assertEquals(AuthFormField.EMAIL, fieldOf("El correo no es válido"))
        assertEquals(AuthFormField.EMAIL, fieldOf("Ya existe una cuenta con ese correo"))
        assertEquals(AuthFormField.PASSWORD, fieldOf("La contraseña debe tener al menos 8 caracteres"))
        assertNull(fieldOf("Algo inesperado"))
    }
}
