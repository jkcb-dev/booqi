package corp.khin.solutions.booqi.data.datasource

import corp.khin.solutions.booqi.data.dto.UserDto

/**
 * TEMPORARY. In-memory stand-in for the `profiles` table until Supabase (#27) lands — see
 * `docs/DATABASE.md`. It reads and writes the same [InMemoryAccountStore] as
 * [FakeAuthRemoteDataSource] (one set of accounts, one current session); "the current user" is
 * whoever that store says is signed in. Replace, don't extend.
 */
class FakeUserProfileRemoteDataSource(
    private val store: InMemoryAccountStore,
) : UserProfileRemoteDataSource {

    override suspend fun findUserById(userId: String): UserDto? = store.usersById[userId]

    override suspend fun updateProfile(displayName: String, photoUrl: String?): UserDto? =
        store.updateCurrent { it.copy(displayName = displayName, photoUrl = photoUrl) }

    override suspend fun updateAddress(line: String, latitude: Double, longitude: Double): UserDto? =
        store.updateCurrent { it.copy(addressLine = line, addressLat = latitude, addressLng = longitude) }

    override suspend fun deleteCurrentAccount(tombstone: UserDto): Boolean {
        val current = store.currentUser() ?: return false
        current.email?.let { store.fingerprintsByEmail.remove(it) }
        current.socialProvider?.let { store.socialAccounts.remove(it) }
        store.usersById[current.id] = tombstone
        store.currentUserId = null
        return true
    }
}
