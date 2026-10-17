@file:OptIn(ExperimentalCoroutinesApi::class)

package corp.khin.solutions.booqi.feature.account

import corp.khin.solutions.booqi.core.common.DomainError
import corp.khin.solutions.booqi.domain.model.ProviderProfile
import corp.khin.solutions.booqi.domain.model.User
import corp.khin.solutions.booqi.domain.usecase.ActualizarPerfilDeUsuarioUseCase
import corp.khin.solutions.booqi.domain.usecase.CerrarSesionUseCase
import corp.khin.solutions.booqi.domain.usecase.ObtenerMiPerfilDeProveedorUseCase
import corp.khin.solutions.booqi.domain.usecase.ObtenerUsuarioActualUseCase
import corp.khin.solutions.booqi.domain.usecase.RequerirCuentaVerificadaUseCase
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
import kotlin.test.assertTrue

/**
 * The profile scenarios of docs/domain/identity-flow.md ("Un visitante explora sin cuenta", "El
 * Usuario actualiza su perfil", "El Usuario cierra sesión", and the access guard in front of "Convertirme en
 * Proveedor") through [ProfileViewModel]'s reducer.
 */
class ProfileViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val users = FakeUserRepository()
    private val profiles = FakeProviderProfileRepository()
    private val ana = User(id = "u1", displayName = "Ana Pérez", email = "ana@booqi.test", isEmailVerified = true)

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun newViewModel() = ProfileViewModel(
        queries = ProfileQueries(
            currentUser = ObtenerUsuarioActualUseCase(users),
            myProviderProfile = ObtenerMiPerfilDeProveedorUseCase(users, profiles),
        ),
        actualizarPerfil = ActualizarPerfilDeUsuarioUseCase(users),
        cerrarSesion = CerrarSesionUseCase(users),
        requerirCuentaVerificada = RequerirCuentaVerificadaUseCase(users),
    )

    private fun started(): ProfileViewModel = newViewModel().also {
        it.onAction(ProfileAction.Start)
        testDispatcher.scheduler.advanceUntilIdle()
    }

    private fun signedIn(user: User = ana) = users.seed(user, signedIn = true)

    // --- Escenario: "Un visitante explora sin cuenta" ---------------------------------------------

    @Test
    fun `a visitor without a session sees the profile as a visitor and no error`() = runTest(testDispatcher) {
        val viewModel = started()

        val state = viewModel.state.value
        assertFalse(state.isLoading)
        assertNull(state.user)
        assertNull(state.error)
    }

    @Test
    fun `before it loads the profile is loading and not a visitor`() {
        assertTrue(newViewModel().state.value.isLoading)
    }

    @Test
    fun `a signed in user sees their name and email and no provider profile yet`() = runTest(testDispatcher) {
        signedIn()

        val viewModel = started()

        assertEquals(ana, viewModel.state.value.user)
        assertFalse(viewModel.state.value.hasProviderProfile)
    }

    @Test
    fun `a user who is already a provider is told so`() = runTest(testDispatcher) {
        signedIn()
        profiles.seed(ProviderProfile(id = "profile-1", userId = "u1"))

        val viewModel = started()

        assertTrue(viewModel.state.value.hasProviderProfile)
    }

    @Test
    fun `a failing load is an error and retrying recovers`() = runTest(testDispatcher) {
        signedIn()
        users.readFailure = DomainError.NoConnection
        val viewModel = started()
        assertEquals(DomainError.NoConnection, viewModel.state.value.error)
        assertFalse(viewModel.state.value.isLoading)

        users.readFailure = null
        viewModel.onAction(ProfileAction.Start)
        advanceUntilIdle()

        assertNull(viewModel.state.value.error)
        assertEquals(ana, viewModel.state.value.user)
    }

    /** The ViewModel outlives a visit: another account entering must not see the previous one. */
    @Test
    fun `entering again after the account changed shows the new account`() = runTest(testDispatcher) {
        signedIn()
        val viewModel = started()
        users.signOut()
        users.seed(User(id = "u2", displayName = "Bea", email = "bea@booqi.test", isEmailVerified = true), signedIn = true)

        viewModel.onAction(ProfileAction.Start)
        advanceUntilIdle()

        assertEquals("Bea", viewModel.state.value.user?.displayName)
    }

    // --- Escenario: "El Usuario actualiza su perfil" ----------------------------------------------

    @Test
    fun `editing opens the form with the current name and photo`() = runTest(testDispatcher) {
        signedIn(ana.copy(photoUrl = "https://example.com/ana.jpg"))
        val viewModel = started()

        viewModel.onAction(ProfileAction.EditClicked)

        val state = viewModel.state.value
        assertTrue(state.isEditing)
        assertEquals("Ana Pérez", state.nameInput)
        assertEquals("https://example.com/ana.jpg", state.photoUrlInput)
    }

    @Test
    fun `saving a new name and photo updates the user`() = runTest(testDispatcher) {
        signedIn()
        val viewModel = started()
        viewModel.onAction(ProfileAction.EditClicked)
        viewModel.onAction(ProfileAction.NameChanged("  Ana María "))
        viewModel.onAction(ProfileAction.PhotoUrlChanged("https://example.com/nueva.jpg"))

        viewModel.onAction(ProfileAction.SaveClicked)
        advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse(state.isEditing)
        assertFalse(state.isSaving)
        assertEquals("Ana María", state.user?.displayName)
        assertEquals("https://example.com/nueva.jpg", state.user?.photoUrl)
        assertEquals("Ana María", users.current?.displayName)
        assertEquals("Perfil actualizado", state.notice)
    }

    @Test
    fun `saving with an empty name is an error on the name field and changes nothing`() = runTest(testDispatcher) {
        signedIn()
        val viewModel = started()
        viewModel.onAction(ProfileAction.EditClicked)
        viewModel.onAction(ProfileAction.NameChanged("   "))

        viewModel.onAction(ProfileAction.SaveClicked)
        advanceUntilIdle()

        assertEquals("El nombre es obligatorio", viewModel.state.value.nameError)
        assertTrue(viewModel.state.value.isEditing)
        assertEquals("Ana Pérez", users.current?.displayName)
    }

    @Test
    fun `cancelling the edit drops the typed values`() = runTest(testDispatcher) {
        signedIn()
        val viewModel = started()
        viewModel.onAction(ProfileAction.EditClicked)
        viewModel.onAction(ProfileAction.NameChanged("Otro"))

        viewModel.onAction(ProfileAction.CancelEditClicked)

        assertFalse(viewModel.state.value.isEditing)
        assertEquals("Ana Pérez", viewModel.state.value.user?.displayName)
    }

    // --- Escenario: "El Usuario cierra sesión" -----------------------------------------------------

    @Test
    fun `signing out ends the session and asks the app to browse as a visitor`() = runTest(testDispatcher) {
        signedIn()
        val viewModel = started()

        val event = eventAfter(viewModel.events) { viewModel.onAction(ProfileAction.SignOutClicked) }

        assertEquals(ProfileEvent.SignedOut, event)
        assertNull(users.current)
        assertFalse(viewModel.state.value.isSigningOut)
    }

    // --- The access guard in front of "Convertirme en Proveedor" -----------------------------------

    @Test
    fun `the provider entry of a verified user opens provider mode`() = runTest(testDispatcher) {
        signedIn()
        val viewModel = started()

        val event = eventAfter(viewModel.events) { viewModel.onAction(ProfileAction.ProviderEntryClicked) }

        assertEquals(ProfileEvent.NavigateToProviderMode, event)
    }

    /** Escenario: "Un visitante intenta reservar sin cuenta" applied to Provider mode. */
    @Test
    fun `the provider entry without a session asks to sign in`() = runTest(testDispatcher) {
        val viewModel = started()

        val event = eventAfter(viewModel.events) { viewModel.onAction(ProfileAction.ProviderEntryClicked) }

        assertEquals(ProfileEvent.NavigateToSignIn, event)
    }

    /** Escenario: "Un Usuario con correo sin verificar intenta ... activar el modo Proveedor". */
    @Test
    fun `the provider entry with an unverified email shows the verification notice`() = runTest(testDispatcher) {
        signedIn(ana.copy(isEmailVerified = false))
        val viewModel = started()

        val event = eventAfter(viewModel.events) { viewModel.onAction(ProfileAction.ProviderEntryClicked) }

        assertEquals(ProfileEvent.NavigateToVerifyEmail, event)
    }

    @Test
    fun `the sign in sign up and verification buttons are events`() = runTest(testDispatcher) {
        val viewModel = started()

        assertEquals(
            ProfileEvent.NavigateToSignIn,
            eventAfter(viewModel.events) { viewModel.onAction(ProfileAction.SignInClicked) },
        )
        assertEquals(
            ProfileEvent.NavigateToSignUp,
            eventAfter(viewModel.events) { viewModel.onAction(ProfileAction.SignUpClicked) },
        )
        assertEquals(
            ProfileEvent.NavigateToVerifyEmail,
            eventAfter(viewModel.events) { viewModel.onAction(ProfileAction.VerifyEmailClicked) },
        )
        assertNotNull(viewModel.state.value)
    }
}
