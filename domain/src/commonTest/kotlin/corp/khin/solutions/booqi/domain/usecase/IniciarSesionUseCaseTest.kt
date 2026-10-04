package corp.khin.solutions.booqi.domain.usecase

import corp.khin.solutions.booqi.domain.model.Credentials
import corp.khin.solutions.booqi.domain.model.Registration
import corp.khin.solutions.booqi.domain.model.SocialProvider
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

/** The sign-in command of docs/domain/identity-flow.md (event "Se inició sesión"). */
class IniciarSesionUseCaseTest {

    private val users = FakeUserRepository()
    private val registrar = RegistrarUsuarioUseCase(users)
    private val iniciarSesion = IniciarSesionUseCase(users)
    private val cerrarSesion = CerrarSesionUseCase(users)

    private suspend fun accountWithSignedOutSession() {
        registrar(Registration.WithEmail("Ana", "ana@example.com", "secreto123")).value()
        cerrarSesion().value()
    }

    @Test
    fun `signing in with the right email and password starts the session`() = runTest {
        accountWithSignedOutSession()

        val user = iniciarSesion(Credentials.WithEmail("ana@example.com", "secreto123")).value()

        assertEquals("Ana", user.displayName)
        assertEquals(user, users.getCurrentUser().value())
    }

    @Test
    fun `an unverified email account can sign in`() = runTest {
        accountWithSignedOutSession()

        assertFalse(iniciarSesion(Credentials.WithEmail("ana@example.com", "secreto123")).value().isEmailVerified)
    }

    @Test
    fun `the email is matched ignoring case and surrounding spaces`() = runTest {
        accountWithSignedOutSession()

        iniciarSesion(Credentials.WithEmail("  ANA@Example.com ", "secreto123")).value()
    }

    @Test
    fun `a wrong password or an unknown email is Unauthorized and leaves the visitor signed out`() = runTest {
        accountWithSignedOutSession()

        iniciarSesion(Credentials.WithEmail("ana@example.com", "otra-clave")).assertUnauthorized()
        iniciarSesion(Credentials.WithEmail("nadie@example.com", "secreto123")).assertUnauthorized()

        assertNull(users.getCurrentUser().value())
    }

    @Test
    fun `an empty email or password is rejected before asking the repository`() = runTest {
        iniciarSesion(Credentials.WithEmail("", "secreto123")).invalidInput()
        iniciarSesion(Credentials.WithEmail("ana@example.com", "")).invalidInput()
    }

    @Test
    fun `signing in with a social provider returns the same verified account each time`() = runTest {
        val first = iniciarSesion(Credentials.WithSocial(SocialProvider.APPLE)).value()
        cerrarSesion().value()
        val second = iniciarSesion(Credentials.WithSocial(SocialProvider.APPLE)).value()

        assertEquals(first, second)
        assertEquals(true, second.isEmailVerified)
    }
}
