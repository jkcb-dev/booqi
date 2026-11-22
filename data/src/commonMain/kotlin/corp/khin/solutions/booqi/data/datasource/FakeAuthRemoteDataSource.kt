package corp.khin.solutions.booqi.data.datasource

import corp.khin.solutions.booqi.data.dto.UserDto

/**
 * TEMPORARY. In-memory stand-in for Supabase Auth until #27 lands — see `docs/DATABASE.md` and
 * [InMemoryAccountStore], which holds the state it shares with [FakeUserProfileRemoteDataSource] and
 * documents the limits (no real hashing, no tokens, forgets everything). Replace, don't extend.
 *
 * The social providers are simulated: the "provider's result" is a fixed name, photo and email per
 * provider, with the email already verified. Sent emails are recorded in the store's
 * `verificationEmailsSentTo` so tests can observe them.
 */
class FakeAuthRemoteDataSource(
    private val store: InMemoryAccountStore = InMemoryAccountStore(),
) : AuthRemoteDataSource {

    override suspend fun currentUser(): UserDto? = store.currentUser()

    override suspend fun signUpWithEmail(name: String, email: String, password: String): UserDto? {
        if (email in store.fingerprintsByEmail) return null
        val user = UserDto(
            id = store.nextUserId(),
            displayName = name,
            email = email,
            photoUrl = null,
            isEmailVerified = false,
            socialProvider = null,
            isDeleted = false,
        )
        store.usersById[user.id] = user
        store.fingerprintsByEmail[email] = fingerprint(email, password)
        store.currentUserId = user.id
        store.recordVerificationEmail(email)
        return user
    }

    override suspend fun signInWithEmail(email: String, password: String): UserDto? {
        val matches = store.fingerprintsByEmail[email] == fingerprint(email, password)
        val user = store.usersById.values.firstOrNull { matches && it.email == email && !it.isDeleted }
        store.currentUserId = user?.id ?: store.currentUserId
        return user
    }

    override suspend fun signInWithSocial(provider: String): UserDto {
        val existing = store.socialAccounts[provider]?.let { store.usersById[it] }
        val user = existing ?: UserDto(
            id = store.nextUserId(),
            displayName = "Usuario ${provider.replaceFirstChar { it.uppercase() }}",
            email = "usuario@$provider.test",
            photoUrl = "https://example.test/avatar/$provider.jpg",
            isEmailVerified = true,
            socialProvider = provider,
            isDeleted = false,
        ).also {
            store.usersById[it.id] = it
            store.socialAccounts[provider] = it.id
        }
        store.currentUserId = user.id
        return user
    }

    override suspend fun signOut() {
        store.currentUserId = null
    }

    override suspend fun confirmEmail(): UserDto? = store.updateCurrent { it.copy(isEmailVerified = true) }

    override suspend fun resendVerificationEmail(): Boolean {
        val email = store.currentUser()?.email ?: return false
        store.recordVerificationEmail(email)
        return true
    }
}
