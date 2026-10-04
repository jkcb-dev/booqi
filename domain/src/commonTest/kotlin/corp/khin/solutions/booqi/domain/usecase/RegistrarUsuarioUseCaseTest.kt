package corp.khin.solutions.booqi.domain.usecase

import corp.khin.solutions.booqi.domain.model.Registration
import corp.khin.solutions.booqi.domain.model.SocialProvider
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Escenarios de docs/domain/identity-flow.md: "Un visitante se registra con Google, Facebook o
 * Apple", "...con correo y contraseña" y "El registro sin nombre se rechaza".
 */
class RegistrarUsuarioUseCaseTest {

    private val users = FakeUserRepository()
    private val registrar = RegistrarUsuarioUseCase(users)

    private fun email(name: String = "Ana", email: String = "ana@example.com", password: String = "secreto123") =
        Registration.WithEmail(name, email, password)

    @Test
    fun `registering with email creates the account signs in and sends a verification email`() = runTest {
        val user = registrar(email()).value()

        assertEquals("Ana", user.displayName)
        assertEquals("ana@example.com", user.email)
        assertFalse(user.isEmailVerified)
        assertNull(user.socialProvider)
        assertEquals(user, users.getCurrentUser().value())
        assertEquals(listOf("ana@example.com"), users.verificationEmailsSentTo)
    }

    @Test
    fun `name and email are stored trimmed and the email lowercase`() = runTest {
        val user = registrar(email(name = "  Ana Pérez ", email = "  Ana@Example.COM ")).value()

        assertEquals("Ana Pérez", user.displayName)
        assertEquals("ana@example.com", user.email)
    }

    @Test
    fun `registering with a social provider prefills name and photo and counts the email as verified`() = runTest {
        for (provider in SocialProvider.entries) {
            val user = FakeUserRepository().let { RegistrarUsuarioUseCase(it)(Registration.WithSocial(provider)) }
                .value()

            assertTrue(user.displayName.isNotBlank(), "$provider name")
            assertNotNull(user.photoUrl, "$provider photo")
            assertTrue(user.isEmailVerified, "$provider verified")
            assertEquals(provider, user.socialProvider)
        }
    }

    @Test
    fun `a social registration starts the session`() = runTest {
        val user = registrar(Registration.WithSocial(SocialProvider.GOOGLE)).value()

        assertEquals(user, users.getCurrentUser().value())
        assertTrue(users.verificationEmailsSentTo.isEmpty())
    }

    @Test
    fun `registering without a name is rejected saying the name is required and nothing is stored`() = runTest {
        for (name in listOf("", "   ")) {
            val message = registrar(email(name = name)).invalidInput()

            assertEquals("El nombre es obligatorio", message)
        }
        assertEquals(0, users.writeCount)
        assertNull(users.getCurrentUser().value())
    }

    @Test
    fun `an invalid email or a short password is rejected before anything is stored`() = runTest {
        for (bad in listOf("", "ana", "ana@", "@example.com", "ana@example", "a na@example.com", "a@@b.com")) {
            registrar(email(email = bad)).invalidInput()
        }
        val message = registrar(email(password = "corta12")).invalidInput()

        assertTrue(message.contains("8"), message)
        assertEquals(0, users.writeCount)
    }

    @Test
    fun `the name is checked before the email and the password`() = runTest {
        assertEquals("El nombre es obligatorio", registrar(email(name = "", email = "x", password = "1")).invalidInput())
    }

    @Test
    fun `an email that already has an account is rejected`() = runTest {
        registrar(email()).value()

        registrar(email(name = "Otra", email = "ANA@example.com")).invalidInput()
    }

    @Test
    fun `a registration never prints its password`() {
        assertFalse(email(password = "secreto123").toString().contains("secreto123"))
    }
}
