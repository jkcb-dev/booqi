package corp.khin.solutions.booqi.domain.repository

import corp.khin.solutions.booqi.core.common.DomainResult
import corp.khin.solutions.booqi.domain.model.SocialProvider
import corp.khin.solutions.booqi.domain.model.User

/**
 * Domain-owned contract for the Identity context (docs/domain/identity-flow.md): the `User`
 * account and the current session. The implementation (in `data`) decides how accounts are sourced
 * — today a TEMPORARY in-memory fake, later Supabase Auth + the `profiles` table (#27).
 *
 * **The session is part of the repository**, as with an auth SDK: methods that act "as the current
 * user" take no user id and fail with
 * [corp.khin.solutions.booqi.core.common.DomainError.Unauthorized] when nobody is signed in.
 * Input validation (name required, email shape, password length) is the use cases' job and has
 * already happened; the repository only reports what storage can know — an email already taken is
 * `InvalidInput`, wrong credentials are `Unauthorized`.
 */
interface UserRepository {

    /** The signed-in user, or `null` while browsing without an account (a normal state). */
    suspend fun getCurrentUser(): DomainResult<User?>

    /** Any account by id (including a deleted tombstone), or `NotFound`. */
    suspend fun getUser(userId: String): DomainResult<User>

    /**
     * Escenario: "Un visitante se registra con correo y contraseña". Creates the account, starts
     * the session and sends the verification email. The account is unverified. `InvalidInput` if
     * [email] already has an account.
     */
    suspend fun registerWithEmail(name: String, email: String, password: String): DomainResult<User>

    /**
     * Escenario: "Un visitante se registra con Google, Facebook o Apple". Runs the provider's
     * result into an account: creates it with the provider's name and photo and a **verified**
     * email, or returns the existing account of that provider, and starts the session.
     */
    suspend fun signInWithSocial(provider: SocialProvider): DomainResult<User>

    /** Starts the session of the email+password account; `Unauthorized` for wrong credentials. */
    suspend fun signInWithEmail(email: String, password: String): DomainResult<User>

    /** Ends the session. Idempotent: succeeds when nobody is signed in. */
    suspend fun signOut(): DomainResult<Unit>

    /**
     * Marks the current user's email verified (the real implementation refreshes the user after
     * they follow the link in the verification email). Idempotent. `Unauthorized` without a
     * session.
     */
    suspend fun confirmEmail(): DomainResult<User>

    /** Sends the verification email again to the current user. `Unauthorized` without a session. */
    suspend fun resendVerificationEmail(): DomainResult<Unit>

    /** Sets the current user's name and photo (already validated/normalized). */
    suspend fun updateProfile(displayName: String, photoUrl: String?): DomainResult<User>

    /**
     * Anonymizes the current user (see [User.anonymized]), removes their login credentials and
     * ends the session. The id stays, as a tombstone. Callers have already checked that no active
     * Bookings block it.
     */
    suspend fun deleteAccount(): DomainResult<Unit>
}
