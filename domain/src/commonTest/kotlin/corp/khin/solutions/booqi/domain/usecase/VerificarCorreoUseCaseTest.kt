package corp.khin.solutions.booqi.domain.usecase

import corp.khin.solutions.booqi.domain.model.Registration
import corp.khin.solutions.booqi.domain.model.SocialProvider
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** "Se verificó el correo" and the resend option of the unverified-email scenario. */
class VerificarCorreoUseCaseTest {

    private val users = FakeUserRepository()
    private val verificar = VerificarCorreoUseCase(users)
    private val reenviar = ReenviarCorreoDeVerificacionUseCase(users)

    private suspend fun registered() =
        RegistrarUsuarioUseCase(users)(Registration.WithEmail("Ana", "ana@example.com", "secreto123")).value()

    @Test
    fun `verifying marks the email verified and it is idempotent`() = runTest {
        registered()

        assertTrue(verificar().value().isEmailVerified)
        assertTrue(verificar().value().isEmailVerified)
        assertTrue(users.getCurrentUser().value()!!.isEmailVerified)
    }

    @Test
    fun `verifying without a session is Unauthorized`() = runTest {
        verificar().assertUnauthorized()
    }

    @Test
    fun `resending sends another verification email to the current user`() = runTest {
        registered()

        reenviar().value()

        assertEquals(listOf("ana@example.com", "ana@example.com"), users.verificationEmailsSentTo)
    }

    @Test
    fun `resending to an already verified account is rejected and sends nothing`() = runTest {
        registered()
        verificar().value()

        reenviar().invalidInput()

        assertEquals(1, users.verificationEmailsSentTo.size)
    }

    @Test
    fun `a social account is verified from the start so there is nothing to resend`() = runTest {
        RegistrarUsuarioUseCase(users)(Registration.WithSocial(SocialProvider.FACEBOOK)).value()

        assertTrue(users.getCurrentUser().value()!!.isEmailVerified)
        reenviar().invalidInput()
        assertFalse(users.verificationEmailsSentTo.isNotEmpty())
    }

    @Test
    fun `resending without a session is Unauthorized`() = runTest {
        reenviar().assertUnauthorized()
    }
}
