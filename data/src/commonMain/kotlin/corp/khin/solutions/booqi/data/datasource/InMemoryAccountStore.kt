package corp.khin.solutions.booqi.data.datasource

import corp.khin.solutions.booqi.data.dto.UserDto

/**
 * TEMPORARY. The one in-memory state behind both [FakeAuthRemoteDataSource] (session) and
 * [FakeUserProfileRemoteDataSource] (profile) until Supabase Auth + the `profiles` table land (#27) —
 * see `docs/DATABASE.md`. It is one store, injected into both fakes, so the accounts and the current
 * session are never duplicated: in the real backend they are one `profiles` row per auth user too.
 * **Real authentication is Supabase's job**: no hashing worth the name, no rate limiting, no token
 * handling, and everything is forgotten when the process ends. Replace, don't extend.
 *
 * Passwords: only a one-way [fingerprint] is kept (never the plaintext, never in a [UserDto], so a
 * log line or heap snapshot doesn't hold one) — enough to tell a right password from a wrong one
 * here, and explicitly **not** a secure hash.
 *
 * [seed] pre-loads accounts (an account with a password can sign in with it; one without is only a
 * name other records point at). It starts signed out. Not thread-safe by design (single process,
 * single writer), like the other fakes.
 */
class InMemoryAccountStore(seed: List<SeedAccount> = emptyList()) {

    internal val usersById = mutableMapOf<String, UserDto>()
    internal val fingerprintsByEmail = mutableMapOf<String, Long>()
    internal val socialAccounts = mutableMapOf<String, String>() // provider -> user id
    internal var currentUserId: String? = null
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

    internal fun nextUserId(): String = "user-${nextId++}"

    internal fun recordVerificationEmail(email: String) {
        sentVerificationEmails += email
    }

    internal fun currentUser(): UserDto? = currentUserId?.let { usersById[it] }

    internal fun updateCurrent(change: (UserDto) -> UserDto): UserDto? =
        currentUserId?.let { usersById[it] }?.let(change)?.also { usersById[it.id] = it }
}

/**
 * An account to pre-load into [InMemoryAccountStore]. [password] is the plaintext sign-in
 * password for [user]'s email, or `null` for an account nobody can sign in to. Test data only.
 */
class SeedAccount(val user: UserDto, val password: String? = null)

private const val FNV_OFFSET = -3750763034362895579L // 64-bit FNV-1a offset basis (0xcbf29ce484222325)
private const val FNV_PRIME = 1099511628211L

// NOT a secure hash: a cheap one-way mix so the plaintext isn't retained. See the class KDoc.
internal fun fingerprint(email: String, password: String): Long =
    "$email\u0000$password".fold(FNV_OFFSET) { acc, c -> (acc xor c.code.toLong()) * FNV_PRIME }
