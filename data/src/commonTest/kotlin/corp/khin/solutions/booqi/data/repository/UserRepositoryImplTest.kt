package corp.khin.solutions.booqi.data.repository

import corp.khin.solutions.booqi.core.common.DomainError
import corp.khin.solutions.booqi.core.common.DomainResult
import corp.khin.solutions.booqi.data.datasource.FakeAuthRemoteDataSource
import corp.khin.solutions.booqi.domain.model.SocialProvider
import corp.khin.solutions.booqi.domain.model.User
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * [UserRepositoryImpl] over the TEMPORARY [FakeAuthRemoteDataSource]: the DTO round trip, the
 * session, the mapping of "no" answers to [DomainError]s and the deletion tombstone. Use case rules
 * (validation, the guard) are covered in `domain`.
 */
class UserRepositoryImplTest {

    private val auth = FakeAuthRemoteDataSource()
    private val repository = UserRepositoryImpl(auth)

    private fun <T> DomainResult<T>.ok(): T = (this as DomainResult.Success<T>).value

    private fun DomainResult<*>.error(): DomainError = (this as DomainResult.Failure).error

    private suspend fun register(email: String = "ana@example.com") =
        repository.registerWithEmail("Ana", email, "secreto123").ok()

    @Test
    fun `nobody is signed in at the start`() = runTest {
        assertNull(repository.getCurrentUser().ok())
    }

    @Test
    fun `registering creates an unverified account starts the session and sends the email`() = runTest {
        val user = register()

        assertEquals(User(id = user.id, displayName = "Ana", email = "ana@example.com"), user)
        assertEquals(user, repository.getCurrentUser().ok())
        assertEquals(listOf("ana@example.com"), auth.verificationEmailsSentTo)
    }

    @Test
    fun `an email that is taken is InvalidInput and creates nothing`() = runTest {
        register()

        assertTrue(repository.registerWithEmail("Otra", "ana@example.com", "otraclave123").error() is DomainError.InvalidInput)
        assertEquals(1, auth.verificationEmailsSentTo.size)
    }

    @Test
    fun `email sign in checks the password`() = runTest {
        val registered = register()
        repository.signOut().ok()

        assertEquals(DomainError.Unauthorized, repository.signInWithEmail("ana@example.com", "mal").error())
        assertEquals(DomainError.Unauthorized, repository.signInWithEmail("x@example.com", "secreto123").error())
        assertNull(repository.getCurrentUser().ok())
        assertEquals(registered, repository.signInWithEmail("ana@example.com", "secreto123").ok())
        assertEquals(registered, repository.getCurrentUser().ok())
    }

    @Test
    fun `a social sign in creates a verified account with name and photo and reuses it afterwards`() = runTest {
        val first = repository.signInWithSocial(SocialProvider.GOOGLE).ok()
        repository.signOut().ok()
        val again = repository.signInWithSocial(SocialProvider.GOOGLE).ok()

        assertEquals(first, again)
        assertTrue(first.isEmailVerified)
        assertEquals(SocialProvider.GOOGLE, first.socialProvider)
        assertTrue(first.displayName.isNotBlank())
        assertTrue(first.photoUrl != null)
        assertFalse(repository.signInWithSocial(SocialProvider.APPLE).ok().id == first.id)
    }

    @Test
    fun `session bound calls without a session are Unauthorized`() = runTest {
        assertEquals(DomainError.Unauthorized, repository.confirmEmail().error())
        assertEquals(DomainError.Unauthorized, repository.resendVerificationEmail().error())
        assertEquals(DomainError.Unauthorized, repository.updateProfile("Ana", null).error())
        assertEquals(DomainError.Unauthorized, repository.deleteAccount().error())
        repository.signOut().ok()
    }

    @Test
    fun `confirming the email and updating the profile change the current user`() = runTest {
        register()

        assertTrue(repository.confirmEmail().ok().isEmailVerified)
        val updated = repository.updateProfile("Ana Pérez", "https://example.com/a.jpg").ok()

        assertEquals("Ana Pérez", updated.displayName)
        assertEquals(updated, repository.getCurrentUser().ok())
        assertEquals(updated, repository.getUser(updated.id).ok())
    }

    @Test
    fun `getUser of an unknown id is NotFound`() = runTest {
        assertEquals(DomainError.NotFound, repository.getUser("missing").error())
    }

    @Test
    fun `deleting keeps an anonymized tombstone ends the session and frees the credentials`() = runTest {
        val registered = register()
        repository.confirmEmail().ok()

        repository.deleteAccount().ok()

        assertNull(repository.getCurrentUser().ok())
        val tombstone = repository.getUser(registered.id).ok()
        assertTrue(tombstone.isDeleted)
        assertEquals("", tombstone.displayName)
        assertNull(tombstone.email)
        assertNull(tombstone.photoUrl)
        assertEquals(User.DELETED_USER_NAME, tombstone.publicName)
        assertEquals(DomainError.Unauthorized, repository.signInWithEmail("ana@example.com", "secreto123").error())
        // The same email can open a brand new account.
        assertFalse(register().id == registered.id)
    }

    @Test
    fun `deleting a social account lets the same provider create a fresh one`() = runTest {
        val first = repository.signInWithSocial(SocialProvider.FACEBOOK).ok()
        repository.deleteAccount().ok()

        val second = repository.signInWithSocial(SocialProvider.FACEBOOK).ok()

        assertFalse(first.id == second.id)
        assertTrue(repository.getUser(first.id).ok().isDeleted)
    }
}
