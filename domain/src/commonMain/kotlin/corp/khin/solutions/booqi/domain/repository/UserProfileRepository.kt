package corp.khin.solutions.booqi.domain.repository

import corp.khin.solutions.booqi.core.common.DomainResult
import corp.khin.solutions.booqi.domain.model.Address
import corp.khin.solutions.booqi.domain.model.User

/**
 * Domain-owned contract for the stored profile of a `User` (docs/domain/identity-flow.md): reading any
 * account, editing the current user's name/photo and saved address, and deleting the account. The
 * session itself (who is signed in) is [SessionRepository]; methods here that act "as the current
 * user" take no user id and fail with [corp.khin.solutions.booqi.core.common.DomainError.Unauthorized]
 * when nobody is signed in. Input is already validated/normalized by the use cases.
 */
interface UserProfileRepository {

    /** Any account by id (including a deleted tombstone), or `NotFound`. */
    suspend fun getUser(userId: String): DomainResult<User>

    /** Sets the current user's name and photo (already validated/normalized). */
    suspend fun updateProfile(displayName: String, photoUrl: String?): DomainResult<User>

    /**
     * Sets the current user's single saved address, replacing any previous one (already validated
     * and normalized — see `AgregarDireccion`/`ActualizarDireccion`). `Unauthorized` without a
     * session. Reading it back is `SessionRepository.getCurrentUser().address`; deleting the account erases it
     * ([deleteAccount]).
     */
    suspend fun saveAddress(address: Address): DomainResult<User>

    /**
     * Anonymizes the current user (see [User.anonymized]), removes their login credentials and
     * ends the session. The id stays, as a tombstone; name, photo, email and saved address are
     * erased. Callers have already checked that no active Bookings block it.
     */
    suspend fun deleteAccount(): DomainResult<Unit>
}
