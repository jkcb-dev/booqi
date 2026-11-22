package corp.khin.solutions.booqi.data.repository

import corp.khin.solutions.booqi.core.common.DomainError
import corp.khin.solutions.booqi.core.common.DomainResult
import corp.khin.solutions.booqi.data.datasource.FakeAuthRemoteDataSource
import corp.khin.solutions.booqi.data.datasource.FakeUserProfileRemoteDataSource
import corp.khin.solutions.booqi.data.datasource.InMemoryAccountStore
import corp.khin.solutions.booqi.data.dto.UserDto
import corp.khin.solutions.booqi.data.mapper.toDomain
import corp.khin.solutions.booqi.data.mapper.toDto
import corp.khin.solutions.booqi.domain.model.Address
import corp.khin.solutions.booqi.domain.model.SocialProvider
import corp.khin.solutions.booqi.domain.model.User
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * [SessionRepositoryImpl] and [UserProfileRepositoryImpl] over the TEMPORARY fakes sharing one [InMemoryAccountStore]: the DTO round trip, the
 * session, the mapping of "no" answers to [DomainError]s and the deletion tombstone. Use case rules
 * (validation, the guard) are covered in `domain`.
 */
class IdentityRepositoriesTest {

    private val store = InMemoryAccountStore()
    private val auth = FakeAuthRemoteDataSource(store)
    private val session = SessionRepositoryImpl(auth)
    private val profiles = UserProfileRepositoryImpl(FakeUserProfileRemoteDataSource(store), auth)

    private val casa = Address("Av. Corrientes 1234, Buenos Aires", -34.6037, -58.3816)
    private val oficina = Address("Av. Santa Fe 900, Buenos Aires", -34.5956, -58.3777)

    private fun <T> DomainResult<T>.ok(): T = (this as DomainResult.Success<T>).value

    private fun DomainResult<*>.error(): DomainError = (this as DomainResult.Failure).error

    private suspend fun register(email: String = "ana@example.com") =
        session.registerWithEmail("Ana", email, "secreto123").ok()

    @Test
    fun `nobody is signed in at the start`() = runTest {
        assertNull(session.getCurrentUser().ok())
    }

    @Test
    fun `registering creates an unverified account starts the session and sends the email`() = runTest {
        val user = register()

        assertEquals(User(id = user.id, displayName = "Ana", email = "ana@example.com"), user)
        assertEquals(user, session.getCurrentUser().ok())
        assertEquals(listOf("ana@example.com"), store.verificationEmailsSentTo)
    }

    @Test
    fun `an email that is taken is InvalidInput and creates nothing`() = runTest {
        register()

        assertTrue(session.registerWithEmail("Otra", "ana@example.com", "otraclave123").error() is DomainError.InvalidInput)
        assertEquals(1, store.verificationEmailsSentTo.size)
    }

    @Test
    fun `email sign in checks the password`() = runTest {
        val registered = register()
        session.signOut().ok()

        assertEquals(DomainError.Unauthorized, session.signInWithEmail("ana@example.com", "mal").error())
        assertEquals(DomainError.Unauthorized, session.signInWithEmail("x@example.com", "secreto123").error())
        assertNull(session.getCurrentUser().ok())
        assertEquals(registered, session.signInWithEmail("ana@example.com", "secreto123").ok())
        assertEquals(registered, session.getCurrentUser().ok())
    }

    @Test
    fun `a social sign in creates a verified account with name and photo and reuses it afterwards`() = runTest {
        val first = session.signInWithSocial(SocialProvider.GOOGLE).ok()
        session.signOut().ok()
        val again = session.signInWithSocial(SocialProvider.GOOGLE).ok()

        assertEquals(first, again)
        assertTrue(first.isEmailVerified)
        assertEquals(SocialProvider.GOOGLE, first.socialProvider)
        assertTrue(first.displayName.isNotBlank())
        assertTrue(first.photoUrl != null)
        assertFalse(session.signInWithSocial(SocialProvider.APPLE).ok().id == first.id)
    }

    @Test
    fun `session bound calls without a session are Unauthorized`() = runTest {
        assertEquals(DomainError.Unauthorized, session.confirmEmail().error())
        assertEquals(DomainError.Unauthorized, session.resendVerificationEmail().error())
        assertEquals(DomainError.Unauthorized, profiles.updateProfile("Ana", null).error())
        assertEquals(DomainError.Unauthorized, profiles.deleteAccount().error())
        assertEquals(DomainError.Unauthorized, profiles.saveAddress(casa).error())
        session.signOut().ok()
    }

    @Test
    fun `confirming the email and updating the profile change the current user`() = runTest {
        register()

        assertTrue(session.confirmEmail().ok().isEmailVerified)
        val updated = profiles.updateProfile("Ana Pérez", "https://example.com/a.jpg").ok()

        assertEquals("Ana Pérez", updated.displayName)
        assertEquals(updated, session.getCurrentUser().ok())
        assertEquals(updated, profiles.getUser(updated.id).ok())
    }

    @Test
    fun `a new account has no address then saving sets it and a second save replaces it`() = runTest {
        val registered = register()
        assertNull(registered.address)

        val first = profiles.saveAddress(casa).ok()
        assertEquals(casa, first.address)
        assertEquals(casa, session.getCurrentUser().ok()?.address)

        val second = profiles.saveAddress(oficina).ok()
        assertEquals(oficina, second.address)
        assertEquals(oficina, profiles.getUser(registered.id).ok().address)
        assertEquals(oficina, auth.currentUser()?.let { Address(it.addressLine!!, it.addressLat!!, it.addressLng!!) })
    }

    @Test
    fun `the address survives signing out and back in`() = runTest {
        register()
        profiles.saveAddress(casa).ok()
        session.signOut().ok()

        assertEquals(casa, session.signInWithEmail("ana@example.com", "secreto123").ok().address)
    }

    @Test
    fun `deleting the account erases the saved address from the stored row`() = runTest {
        val registered = register()
        profiles.saveAddress(casa).ok()

        profiles.deleteAccount().ok()

        assertNull(profiles.getUser(registered.id).ok().address)
        val row = FakeUserProfileRemoteDataSource(store).findUserById(registered.id)!!
        assertNull(row.addressLine)
        assertNull(row.addressLat)
        assertNull(row.addressLng)
    }

    @Test
    fun `a partial address row is read as no address`() = runTest {
        val partial = UserDto("u", "Ana", "a@b.c", null, false, null, false, addressLine = "Calle 1")

        assertNull(partial.toDomain().address)
        assertEquals(casa, User("u", "Ana", "a@b.c", address = casa).toDto().toDomain().address)
    }

    @Test
    fun `getUser of an unknown id is NotFound`() = runTest {
        assertEquals(DomainError.NotFound, profiles.getUser("missing").error())
    }

    @Test
    fun `deleting keeps an anonymized tombstone ends the session and frees the credentials`() = runTest {
        val registered = register()
        session.confirmEmail().ok()

        profiles.deleteAccount().ok()

        assertNull(session.getCurrentUser().ok())
        val tombstone = profiles.getUser(registered.id).ok()
        assertTrue(tombstone.isDeleted)
        assertEquals("", tombstone.displayName)
        assertNull(tombstone.email)
        assertNull(tombstone.photoUrl)
        assertEquals(User.DELETED_USER_NAME, tombstone.publicName)
        assertEquals(DomainError.Unauthorized, session.signInWithEmail("ana@example.com", "secreto123").error())
        // The same email can open a brand new account.
        assertFalse(register().id == registered.id)
    }

    @Test
    fun `deleting a social account lets the same provider create a fresh one`() = runTest {
        val first = session.signInWithSocial(SocialProvider.FACEBOOK).ok()
        profiles.deleteAccount().ok()

        val second = session.signInWithSocial(SocialProvider.FACEBOOK).ok()

        assertFalse(first.id == second.id)
        assertTrue(profiles.getUser(first.id).ok().isDeleted)
    }
}
