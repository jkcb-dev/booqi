@file:OptIn(ExperimentalCoroutinesApi::class)

package corp.khin.solutions.booqi.feature.account

import corp.khin.solutions.booqi.core.common.DomainError
import corp.khin.solutions.booqi.domain.model.User
import corp.khin.solutions.booqi.domain.usecase.ObtenerUsuarioActualUseCase
import corp.khin.solutions.booqi.domain.usecase.ReenviarCorreoDeVerificacionUseCase
import corp.khin.solutions.booqi.domain.usecase.VerificarCorreoUseCase
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
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Escenario "Un Usuario con correo sin verificar intenta reservar" (docs/domain/identity-flow.md):
 * the notice with "reenviar el correo de verificación" and the way out once verified, through
 * [VerifyEmailViewModel]'s reducer.
 */
class VerifyEmailViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val users = FakeUserRepository().apply {
        seed(User(id = "u1", displayName = "Ana", email = "ana@booqi.test"), signedIn = true)
    }

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun newViewModel() = VerifyEmailViewModel(
        obtenerUsuarioActual = ObtenerUsuarioActualUseCase(users),
        verificarCorreo = VerificarCorreoUseCase(users),
        reenviarCorreo = ReenviarCorreoDeVerificacionUseCase(users),
    )

    private fun started() = newViewModel().also {
        it.onAction(VerifyEmailAction.Start)
        testDispatcher.scheduler.advanceUntilIdle()
    }

    @Test
    fun `the notice says where the verification email went`() = runTest(testDispatcher) {
        val viewModel = started()

        assertFalse(viewModel.state.value.isLoading)
        assertEquals("ana@booqi.test", viewModel.state.value.email)
        assertNull(viewModel.state.value.error)
    }

    @Test
    fun `before it loads the notice is loading and not empty`() {
        assertTrue(newViewModel().state.value.isLoading)
    }

    @Test
    fun `resending sends the email again and says so`() = runTest(testDispatcher) {
        val viewModel = started()

        viewModel.onAction(VerifyEmailAction.ResendClicked)
        advanceUntilIdle()

        assertEquals(listOf("ana@booqi.test"), users.verificationEmails)
        assertEquals("Te enviamos el correo de verificación otra vez", viewModel.state.value.notice)
        assertFalse(viewModel.state.value.isResending)
    }

    @Test
    fun `confirming after following the link verifies the account and finishes`() = runTest(testDispatcher) {
        val viewModel = started()

        val event = eventAfter(viewModel.events) { viewModel.onAction(VerifyEmailAction.ConfirmClicked) }

        assertEquals(VerifyEmailEvent.Verified, event)
        assertEquals(true, users.current?.isEmailVerified)
        assertFalse(viewModel.state.value.isChecking)
    }

    @Test
    fun `confirming before following the link keeps the notice with a message`() = runTest(testDispatcher) {
        users.emailLinkFollowed = false
        val viewModel = started()

        viewModel.onAction(VerifyEmailAction.ConfirmClicked)
        advanceUntilIdle()

        assertIs<DomainError.InvalidInput>(viewModel.state.value.error)
        assertEquals(false, users.current?.isEmailVerified)
        assertFalse(viewModel.state.value.isChecking)
    }

    @Test
    fun `later leaves without verifying`() = runTest(testDispatcher) {
        val viewModel = started()

        val event = eventAfter(viewModel.events) { viewModel.onAction(VerifyEmailAction.LaterClicked) }

        assertEquals(VerifyEmailEvent.Close, event)
        assertEquals(false, users.current?.isEmailVerified)
    }

    @Test
    fun `starting on an already verified account finishes at once`() = runTest(testDispatcher) {
        users.seed(User(id = "u2", displayName = "Bea", email = "bea@booqi.test", isEmailVerified = true), signedIn = true)
        val viewModel = newViewModel()

        val event = eventAfter(viewModel.events) { viewModel.onAction(VerifyEmailAction.Start) }

        assertEquals(VerifyEmailEvent.Verified, event)
    }

    @Test
    fun `starting without a session says the session ended`() = runTest(testDispatcher) {
        users.signOut()
        val viewModel = started()

        assertEquals(DomainError.Unauthorized, viewModel.state.value.error)
        assertNull(viewModel.state.value.email)
        assertFalse(viewModel.state.value.isLoading)
    }

    @Test
    fun `entering again drops the messages of the previous visit`() = runTest(testDispatcher) {
        val viewModel = started()
        viewModel.onAction(VerifyEmailAction.ResendClicked)
        advanceUntilIdle()

        viewModel.onAction(VerifyEmailAction.Start)
        advanceUntilIdle()

        assertNull(viewModel.state.value.notice)
    }
}
