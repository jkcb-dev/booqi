package corp.khin.solutions.booqi.data.datasource

import corp.khin.solutions.booqi.data.dto.UserDto

/**
 * Source of accounts and the current session — what Supabase Auth plus the `profiles` table will
 * be (#27, docs/DATABASE.md). `data`-only concern; the DTO never crosses into `domain`.
 *
 * A "no" that is a normal answer (wrong credentials, email taken, nobody signed in) is `null`, not
 * an exception, so the repository can turn it into the right `DomainError`; exceptions are for
 * real failures (network, ...) and are caught at the repository boundary. **Passwords only travel
 * *in* to this boundary** (sign-up, sign-in) — they are never part of a [UserDto] and never come
 * back out.
 */
interface AuthRemoteDataSource {

    /** The user of the current session, or `null` when signed out. */
    suspend fun currentUser(): UserDto?

    /** The account with [userId] (a deleted one comes back anonymized), or `null`. */
    suspend fun findUserById(userId: String): UserDto?

    /**
     * Creates an unverified email+password account, signs it in and sends the verification email;
     * `null` (nothing created) when [email] already has an account. [email] arrives normalized.
     */
    suspend fun signUpWithEmail(name: String, email: String, password: String): UserDto?

    /** Signs in an email+password account; `null` for an unknown email or a wrong password. */
    suspend fun signInWithEmail(email: String, password: String): UserDto?

    /**
     * Completes a social sign-in for [provider] (`"google"`, `"facebook"`, `"apple"`): creates the
     * account (name/photo from the provider, email verified) on first use, signs it in, and returns it.
     */
    suspend fun signInWithSocial(provider: String): UserDto

    suspend fun signOut()

    /** Marks the current user's email verified; `null` when signed out. */
    suspend fun confirmEmail(): UserDto?

    /** Re-sends the verification email to the current user; `false` when signed out. */
    suspend fun resendVerificationEmail(): Boolean

    /** Updates the current user's name and photo; `null` when signed out. */
    suspend fun updateProfile(displayName: String, photoUrl: String?): UserDto?

    /**
     * Replaces the current user's row with [tombstone] (the anonymized account — the row survives so
     * history keeps its reference), removes its login credentials and ends the session. `false`
     * when signed out.
     */
    suspend fun deleteCurrentAccount(tombstone: UserDto): Boolean
}
