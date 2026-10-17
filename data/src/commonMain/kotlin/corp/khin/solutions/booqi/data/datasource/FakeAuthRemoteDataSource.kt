package corp.khin.solutions.booqi.data.datasource

import corp.khin.solutions.booqi.data.dto.UserDto

/**
 * TEMPORARY. In-memory stand-in for Supabase Auth + the `profiles` table until #27 lands — see
 * `docs/DATABASE.md`. **Real authentication is Supabase's job**: this fake does no hashing worth the
 * name, no rate limiting, no token handling, and forgets everything when the process ends. It
 * exists only so the module graph and the Identity flows can be proven end-to-end. Replace, don't
 * extend.
 *
 * Passwords: only a one-way [fingerprint] is kept (never the plaintext, never in a [UserDto], so a
 * log line or heap snapshot of the fake doesn't hold one) — enough to tell a right password from a
 * wrong one here, and explicitly **not** a secure hash.
 *
 * The social providers are simulated: the "provider's result" is a fixed name, photo and email per
 * provider, with the email already verified. Sent emails are recorded in [verificationEmailsSentTo]
 * so tests can observe them. Not thread-safe by design (single process, single writer), like the
 * other fakes.
 *
 * [seed] pre-loads accounts (an account with a password can sign in with it; one without is only a
 * name other records point at), the same way the other fakes take a seed. It starts signed out.
 */
// Mirrors UserRepository one-to-one (account + session + profile columns of a single `profiles` row);
// splitting it only to get under the function-count threshold would scatter that row (see #61).
@Suppress("TooManyFunctions")
class FakeAuthRemoteDataSource(
    seed: List<SeedAccount> = emptyList(),
) : AuthRemoteDataSource {

    private val usersById = mutableMapOf<String, UserDto>()
    private val fingerprintsByEmail = mutableMapOf<String, Long>()
    private val socialAccounts = mutableMapOf<String, String>() // provider -> user id
    private var currentUserId: String? = null
    private var nextId = 1

    private val sentVerificationEmails = mutableListOf<String>()

    init {
        seed.forEach { account ->
            usersById[account.user.id] = account.user
            val email = account.user.email
            if (account.password != null && email != null) {
                fingerprintsByEmail[email] = fingerprint(email, account.password)
            }
        }
    }

    /** Addresses a verification email was sent to, in order (sign-up and re-sends). */
    val verificationEmailsSentTo: List<String> get() = sentVerificationEmails

    override suspend fun currentUser(): UserDto? = currentUserId?.let { usersById[it] }

    override suspend fun findUserById(userId: String): UserDto? = usersById[userId]

    override suspend fun signUpWithEmail(name: String, email: String, password: String): UserDto? {
        if (email in fingerprintsByEmail) return null
        val user = UserDto(
            id = "user-${nextId++}",
            displayName = name,
            email = email,
            photoUrl = null,
            isEmailVerified = false,
            socialProvider = null,
            isDeleted = false,
        )
        usersById[user.id] = user
        fingerprintsByEmail[email] = fingerprint(email, password)
        currentUserId = user.id
        sentVerificationEmails += email
        return user
    }

    override suspend fun signInWithEmail(email: String, password: String): UserDto? {
        val matches = fingerprintsByEmail[email] == fingerprint(email, password)
        val user = usersById.values.firstOrNull { matches && it.email == email && !it.isDeleted }
        currentUserId = user?.id ?: currentUserId
        return user
    }

    override suspend fun signInWithSocial(provider: String): UserDto {
        val existing = socialAccounts[provider]?.let { usersById[it] }
        val user = existing ?: UserDto(
            id = "user-${nextId++}",
            displayName = "Usuario ${provider.replaceFirstChar { it.uppercase() }}",
            email = "usuario@$provider.test",
            photoUrl = "https://example.test/avatar/$provider.jpg",
            isEmailVerified = true,
            socialProvider = provider,
            isDeleted = false,
        ).also {
            usersById[it.id] = it
            socialAccounts[provider] = it.id
        }
        currentUserId = user.id
        return user
    }

    override suspend fun signOut() {
        currentUserId = null
    }

    override suspend fun confirmEmail(): UserDto? =
        usersById.update(currentUserId) { it.copy(isEmailVerified = true) }

    override suspend fun resendVerificationEmail(): Boolean {
        val email = currentUser()?.email ?: return false
        sentVerificationEmails += email
        return true
    }

    override suspend fun updateProfile(displayName: String, photoUrl: String?): UserDto? =
        usersById.update(currentUserId) { it.copy(displayName = displayName, photoUrl = photoUrl) }

    override suspend fun updateAddress(line: String, latitude: Double, longitude: Double): UserDto? =
        usersById.update(currentUserId) {
            it.copy(addressLine = line, addressLat = latitude, addressLng = longitude)
        }

    override suspend fun deleteCurrentAccount(tombstone: UserDto): Boolean {
        val current = currentUser() ?: return false
        current.email?.let { fingerprintsByEmail.remove(it) }
        current.socialProvider?.let { socialAccounts.remove(it) }
        usersById[current.id] = tombstone
        currentUserId = null
        return true
    }

}

/**
 * An account to pre-load into [FakeAuthRemoteDataSource]. [password] is the plaintext sign-in
 * password for [user]'s email, or `null` for an account nobody can sign in to. Test data only.
 */
class SeedAccount(val user: UserDto, val password: String? = null)

private const val FNV_OFFSET = -3750763034362895579L // 64-bit FNV-1a offset basis (0xcbf29ce484222325)
private const val FNV_PRIME = 1099511628211L

// NOT a secure hash: a cheap one-way mix so the plaintext isn't retained. See the class KDoc.
private fun fingerprint(email: String, password: String): Long =
    "$email\u0000$password".fold(FNV_OFFSET) { acc, c -> (acc xor c.code.toLong()) * FNV_PRIME }

private fun MutableMap<String, UserDto>.update(id: String?, change: (UserDto) -> UserDto): UserDto? =
    id?.let { this[it] }?.let(change)?.also { this[it.id] = it }
