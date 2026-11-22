package corp.khin.solutions.booqi.data.datasource

import corp.khin.solutions.booqi.data.dto.UserDto

/**
 * Source of the session side of the Identity context — what Supabase Auth will be (#27,
 * docs/DATABASE.md): sign-up, sign-in/out, email verification and the current user. `data`-only
 * concern; the DTO never crosses into `domain`. The profile columns are [UserProfileRemoteDataSource].
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
}
