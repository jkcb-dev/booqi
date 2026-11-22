package corp.khin.solutions.booqi.data.repository

import corp.khin.solutions.booqi.core.common.DomainError
import corp.khin.solutions.booqi.core.common.DomainResult
import corp.khin.solutions.booqi.core.common.asFailure
import corp.khin.solutions.booqi.core.common.asSuccess
import corp.khin.solutions.booqi.data.datasource.AuthRemoteDataSource
import corp.khin.solutions.booqi.data.datasource.UserProfileRemoteDataSource
import corp.khin.solutions.booqi.data.mapper.toDomain
import corp.khin.solutions.booqi.data.mapper.toDto
import corp.khin.solutions.booqi.domain.model.Address
import corp.khin.solutions.booqi.domain.model.User
import corp.khin.solutions.booqi.domain.repository.UserProfileRepository

/**
 * Maps the profile datasource's answers to [DomainResult]s: a `null` that means "no session" is
 * `Unauthorized`, a missing user `NotFound`. The anonymized shape of a deleted account is decided
 * here from the domain's [User.anonymized], so there is one definition of it. Deleting needs to
 * know who is signed in, which is the session's data, so it also reads [AuthRemoteDataSource].
 */
class UserProfileRepositoryImpl(
    private val profiles: UserProfileRemoteDataSource,
    private val auth: AuthRemoteDataSource,
) : UserProfileRepository {

    override suspend fun getUser(userId: String): DomainResult<User> = guarded {
        profiles.findUserById(userId)?.toDomain()?.asSuccess() ?: DomainError.NotFound.asFailure()
    }

    override suspend fun updateProfile(displayName: String, photoUrl: String?): DomainResult<User> = guarded {
        profiles.updateProfile(displayName, photoUrl)?.toDomain()?.asSuccess() ?: DomainError.Unauthorized.asFailure()
    }

    override suspend fun saveAddress(address: Address): DomainResult<User> = guarded {
        profiles.updateAddress(address.line, address.latitude, address.longitude)?.toDomain()?.asSuccess()
            ?: DomainError.Unauthorized.asFailure()
    }

    override suspend fun deleteAccount(): DomainResult<Unit> = guarded {
        val current = auth.currentUser() ?: return@guarded DomainError.Unauthorized.asFailure()
        if (profiles.deleteCurrentAccount(current.toDomain().anonymized().toDto())) {
            Unit.asSuccess()
        } else {
            DomainError.Unauthorized.asFailure()
        }
    }
}
