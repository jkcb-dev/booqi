package corp.khin.solutions.booqi.domain.usecase

import corp.khin.solutions.booqi.domain.model.Registration
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertNull

/**
 * Escenarios: "El Usuario cierra sesión" y "Un visitante explora sin cuenta"
 * (docs/domain/identity-flow.md) — the session half. The public content itself is the Catalog's.
 */
class CerrarSesionUseCaseTest {

    private val users = FakeUserRepository()
    private val cerrarSesion = CerrarSesionUseCase(users)
    private val usuarioActual = ObtenerUsuarioActualUseCase(users)
    private val guard = RequerirCuentaVerificadaUseCase(users)

    @Test
    fun `a visitor with no account has no current user and that is not an error`() = runTest {
        assertNull(usuarioActual().value())
    }

    @Test
    fun `after signing out the user is a visitor again and cannot book until signing in`() = runTest {
        RegistrarUsuarioUseCase(users)(Registration.WithEmail("Ana", "ana@example.com", "secreto123")).value()
        users.confirmEmail().value()
        guard().value()

        cerrarSesion().value()

        assertNull(usuarioActual().value())
        guard().assertUnauthorized()
    }

    @Test
    fun `signing out with no session succeeds`() = runTest {
        cerrarSesion().value()
    }
}
