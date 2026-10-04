package corp.khin.solutions.booqi.domain.usecase

import corp.khin.solutions.booqi.core.common.DomainError
import corp.khin.solutions.booqi.core.common.DomainResult
import corp.khin.solutions.booqi.core.common.asFailure
import corp.khin.solutions.booqi.core.common.asSuccess
import corp.khin.solutions.booqi.domain.model.SocialProvider
import corp.khin.solutions.booqi.domain.model.User
import corp.khin.solutions.booqi.domain.repository.UserRepository
import kotlin.test.assertEquals
import kotlin.test.assertIs

/**
 * Hand-written in-memory [UserRepository] for the Identity use case tests, honoring the contract
 * documented on the interface (session-bound methods fail `Unauthorized` when signed out, a taken
 * email is `InvalidInput`, a deleted account stays as a tombstone). [writeCount] lets a test assert
 * that a rejected action never reached storage; [verificationEmailsSentTo] records sent emails.
 * Test-only: plaintext passwords here never leave the test.
 */
class FakeUserRepository : UserRepository {

    private val usersById = mutableMapOf<String, User>()
    private val passwordsByEmail = mutableMapOf<String, String>()
    private var currentId: String? = null
    private var nextId = 1

    var writeCount = 0
        private set

    val verificationEmailsSentTo = mutableListOf<String>()

    /** Stores [user] as-is and signs it in (no write counted), for arranging a "Dado que". */
    fun seedSignedIn(user: User): User = user.also {
        usersById[it.id] = it
        currentId = it.id
    }

    fun stored(userId: String): User = usersById.getValue(userId)

    override suspend fun getCurrentUser(): DomainResult<User?> = currentId?.let { usersById[it] }.asSuccess()

    override suspend fun getUser(userId: String): DomainResult<User> =
        usersById[userId]?.asSuccess() ?: DomainError.NotFound.asFailure()

    override suspend fun registerWithEmail(name: String, email: String, password: String): DomainResult<User> {
        writeCount++
        if (email in passwordsByEmail) {
            return DomainError.InvalidInput("Ya existe una cuenta con ese correo").asFailure()
        }
        val user = User(id = "user-${nextId++}", displayName = name, email = email)
        usersById[user.id] = user
        passwordsByEmail[email] = password
        currentId = user.id
        verificationEmailsSentTo += email
        return user.asSuccess()
    }

    override suspend fun signInWithSocial(provider: SocialProvider): DomainResult<User> {
        writeCount++
        val user = usersById.values.firstOrNull { it.socialProvider == provider && !it.isDeleted }
            ?: User(
                id = "user-${nextId++}",
                displayName = "Usuario ${provider.name.lowercase()}",
                email = "usuario@${provider.name.lowercase()}.test",
                photoUrl = "https://example.test/${provider.name.lowercase()}.jpg",
                isEmailVerified = true,
                socialProvider = provider,
            ).also { usersById[it.id] = it }
        currentId = user.id
        return user.asSuccess()
    }

    override suspend fun signInWithEmail(email: String, password: String): DomainResult<User> {
        if (passwordsByEmail[email] != password) return DomainError.Unauthorized.asFailure()
        val user = usersById.values.first { it.email == email }
        currentId = user.id
        return user.asSuccess()
    }

    override suspend fun signOut(): DomainResult<Unit> {
        currentId = null
        return Unit.asSuccess()
    }

    override suspend fun confirmEmail(): DomainResult<User> = updateCurrent { it.copy(isEmailVerified = true) }

    override suspend fun resendVerificationEmail(): DomainResult<Unit> {
        val email = currentId?.let { usersById[it]?.email } ?: return DomainError.Unauthorized.asFailure()
        verificationEmailsSentTo += email
        return Unit.asSuccess()
    }

    override suspend fun updateProfile(displayName: String, photoUrl: String?): DomainResult<User> =
        updateCurrent { it.copy(displayName = displayName, photoUrl = photoUrl) }

    override suspend fun deleteAccount(): DomainResult<Unit> {
        writeCount++
        val current = currentId?.let { usersById[it] } ?: return DomainError.Unauthorized.asFailure()
        current.email?.let { passwordsByEmail.remove(it) }
        usersById[current.id] = current.anonymized()
        currentId = null
        return Unit.asSuccess()
    }

    private fun updateCurrent(change: (User) -> User): DomainResult<User> {
        writeCount++
        val current = currentId?.let { usersById[it] } ?: return DomainError.Unauthorized.asFailure()
        return change(current).also { usersById[it.id] = it }.asSuccess()
    }
}

internal fun DomainResult<*>.assertUnauthorized() {
    assertIs<DomainResult.Failure>(this)
    assertEquals(DomainError.Unauthorized, error)
}
