package corp.khin.solutions.booqi.domain.usecase

import corp.khin.solutions.booqi.domain.model.User
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** The user → profile path of the providerId contract (docs/DOMAIN.md). */
class ObtenerMiPerfilDeProveedorUseCaseTest {

    private val users = FakeUserRepository()
    private val profiles = FakeProviderProfileRepository()
    private val obtener = ObtenerMiPerfilDeProveedorUseCase(users, profiles)

    @Test
    fun `a user who never activated Provider mode has no profile and that is not an error`() = runTest {
        users.seedSignedIn(User("user-1", "Ana", "ana@example.com", isEmailVerified = true))

        assertNull(obtener().value())
    }

    @Test
    fun `after activating Provider mode the profile id is the providerId and differs from the user id`() = runTest {
        users.seedSignedIn(User("user-1", "Ana", "ana@example.com", isEmailVerified = true))
        val activated = ActivarModoProveedorUseCase(profiles)("user-1").value()

        val mine = obtener().value()!!

        assertEquals(activated.id, mine.id)
        assertEquals("user-1", mine.userId)
        assertEquals(false, mine.id == "user-1")
    }

    @Test
    fun `without a session it is Unauthorized`() = runTest {
        obtener().assertUnauthorized()
    }
}
