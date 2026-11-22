package corp.khin.solutions.booqi.data.datasource

import corp.khin.solutions.booqi.data.dto.UserDto

/**
 * Source of the stored profile of a user — the `profiles` table (#27, docs/DATABASE.md): lookup by
 * id, the current user's name/photo and saved address, and account deletion. Who "the current user"
 * is comes from the session, which is [AuthRemoteDataSource]'s concern; as there, a normal "no"
 * (nobody signed in) is `null`/`false`, not an exception.
 */
interface UserProfileRemoteDataSource {

    /** The account with [userId] (a deleted one comes back anonymized), or `null`. */
    suspend fun findUserById(userId: String): UserDto?

    /** Updates the current user's name and photo; `null` when signed out. */
    suspend fun updateProfile(displayName: String, photoUrl: String?): UserDto?

    /**
     * Sets the current user's saved address (`address_line`/`address_lat`/`address_lng`, replacing
     * any previous one); `null` when signed out.
     */
    suspend fun updateAddress(line: String, latitude: Double, longitude: Double): UserDto?

    /**
     * Replaces the current user's row with [tombstone] (the anonymized account — the row survives so
     * history keeps its reference), removes its login credentials and ends the session. `false`
     * when signed out.
     */
    suspend fun deleteCurrentAccount(tombstone: UserDto): Boolean
}
