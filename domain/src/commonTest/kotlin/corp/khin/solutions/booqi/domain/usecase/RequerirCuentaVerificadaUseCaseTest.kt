package corp.khin.solutions.booqi.domain.usecase

import corp.khin.solutions.booqi.domain.model.Registration
import corp.khin.solutions.booqi.domain.model.SocialProvider
import corp.khin.solutions.booqi.domain.model.User
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Escenarios: "Un visitante intenta reservar sin cuenta" y "Un Usuario con correo sin verificar
 * intenta reservar" (docs/domain/identity-flow.md) — the access guard in front of requesting a
 * booking and activating Provider mode.
 */
class RequerirCuentaVerificadaUseCaseTest {

    private val users = FakeUserRepository()
    private val guard = RequerirCuentaVerificadaUseCase(users)

    @Test
    fun `a visitor without a session is Unauthorized so the UI asks them to sign in`() = runTest {
        guard().assertUnauthorized()
    }

    @Test
    fun `a signed in user with an unverified email is rejected telling them to verify`() = runTest {
        RegistrarUsuarioUseCase(users)(Registration.WithEmail("Ana", "ana@example.com", "secreto123")).value()

        assertEquals(RequerirCuentaVerificadaUseCase.EMAIL_NOT_VERIFIED_MESSAGE, guard().invalidInput())
    }

    @Test
    fun `once the email is verified the guard hands back the user`() = runTest {
        val registered = RegistrarUsuarioUseCase(users)(Registration.WithEmail("Ana", "ana@example.com", "secreto123"))
            .value()
        VerificarCorreoUseCase(users)().value()

        assertEquals(registered.id, guard().value().id)
    }

    @Test
    fun `a social sign in counts as verified from the start`() = runTest {
        val google = RegistrarUsuarioUseCase(users)(Registration.WithSocial(SocialProvider.GOOGLE)).value()

        assertEquals(google, guard().value())
    }

    @Test
    fun `the pure rule never lets a deleted account through`() {
        val deleted = User("user-1", "Ana", "ana@example.com", isEmailVerified = true).anonymized()

        RequerirCuentaVerificadaUseCase.guard(deleted).assertUnauthorized()
        RequerirCuentaVerificadaUseCase.guard(null).assertUnauthorized()
    }
}
