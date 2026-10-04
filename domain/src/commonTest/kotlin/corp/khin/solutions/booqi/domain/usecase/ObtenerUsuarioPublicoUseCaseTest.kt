package corp.khin.solutions.booqi.domain.usecase

import corp.khin.solutions.booqi.domain.model.User
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** The display rule for people other than the signed-in user, including deleted accounts. */
class ObtenerUsuarioPublicoUseCaseTest {

    private val users = FakeUserRepository()
    private val obtener = ObtenerUsuarioPublicoUseCase(users)

    @Test
    fun `a normal account shows its name and photo but not its email`() = runTest {
        users.seedSignedIn(User("user-1", "Ana", "ana@example.com", photoUrl = "https://example.com/ana.jpg"))

        val public = obtener("user-1").value()

        assertEquals("Ana", public.displayName)
        assertEquals("https://example.com/ana.jpg", public.photoUrl)
        assertEquals(false, public.isDeleted)
    }

    @Test
    fun `a deleted account is shown as Usuario eliminado with no photo`() = runTest {
        users.seedSignedIn(User("user-1", "Ana", "ana@example.com", photoUrl = "https://example.com/ana.jpg"))
        users.deleteAccount().value()

        val public = obtener("user-1").value()

        assertEquals("Usuario eliminado", public.displayName)
        assertEquals(User.DELETED_USER_NAME, public.displayName)
        assertNull(public.photoUrl)
        assertEquals(true, public.isDeleted)
    }

    @Test
    fun `an id with no account is NotFound`() = runTest {
        obtener("missing").assertNotFound()
    }
}
