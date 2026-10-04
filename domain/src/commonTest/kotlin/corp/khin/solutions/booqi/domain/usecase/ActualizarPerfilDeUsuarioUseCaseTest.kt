package corp.khin.solutions.booqi.domain.usecase

import corp.khin.solutions.booqi.domain.model.Registration
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** Escenario: "El Usuario actualiza su perfil" (docs/domain/identity-flow.md). */
class ActualizarPerfilDeUsuarioUseCaseTest {

    private val users = FakeUserRepository()
    private val actualizar = ActualizarPerfilDeUsuarioUseCase(users)

    private suspend fun signedIn() =
        RegistrarUsuarioUseCase(users)(Registration.WithEmail("Ana", "ana@example.com", "secreto123")).value()

    @Test
    fun `changing name and photo updates the signed in user`() = runTest {
        val before = signedIn()

        val after = actualizar("Ana Pérez", "https://example.com/ana.jpg").value()

        assertEquals("Ana Pérez", after.displayName)
        assertEquals("https://example.com/ana.jpg", after.photoUrl)
        assertEquals(before.id, after.id)
        assertEquals(after, users.getCurrentUser().value())
    }

    @Test
    fun `name and photo are trimmed and a blank photo clears it`() = runTest {
        signedIn()
        actualizar("Ana", "https://example.com/ana.jpg").value()

        val after = actualizar("  Ana María ", "   ").value()

        assertEquals("Ana María", after.displayName)
        assertNull(after.photoUrl)
    }

    @Test
    fun `the name stays required`() = runTest {
        signedIn()
        val writes = users.writeCount

        assertEquals("El nombre es obligatorio", actualizar("  ").invalidInput())

        assertEquals(writes, users.writeCount)
        assertEquals("Ana", users.getCurrentUser().value()!!.displayName)
    }

    @Test
    fun `updating without a session is Unauthorized`() = runTest {
        actualizar("Ana").assertUnauthorized()
    }
}
