package corp.khin.solutions.booqi.data.repository

import corp.khin.solutions.booqi.core.common.DomainError
import corp.khin.solutions.booqi.core.common.DomainResult
import corp.khin.solutions.booqi.core.common.asFailure
import corp.khin.solutions.booqi.core.common.asSuccess
import corp.khin.solutions.booqi.data.datasource.AuthRemoteDataSource
import corp.khin.solutions.booqi.data.mapper.toDomain
import corp.khin.solutions.booqi.data.mapper.toDto
import corp.khin.solutions.booqi.data.mapper.toStorageCode
import corp.khin.solutions.booqi.domain.model.SocialProvider
import corp.khin.solutions.booqi.domain.model.User
import corp.khin.solutions.booqi.domain.repository.UserRepository

/**
 * Maps the auth datasource's answers to [DomainResult]s: a `null` that means "wrong credentials" or
 * "no session" is `Unauthorized`, an email that is already taken is `InvalidInput`, a missing user
 * `NotFound`. The anonymized shape of a deleted account is decided here from the domain's
 * [User.anonymized], so there is one definition of it.
 */
class UserRepositoryImpl(
    private val auth: AuthRemoteDataSource,
) : UserRepository {

    override suspend fun getCurrentUser(): DomainResult<User?> = guarded {
        auth.currentUser()?.toDomain().asSuccess()
    }

    override suspend fun getUser(userId: String): DomainResult<User> = guarded {
        auth.findUserById(userId)?.toDomain()?.asSuccess() ?: DomainError.NotFound.asFailure()
    }

    override suspend fun registerWithEmail(name: String, email: String, password: String): DomainResult<User> =
        guarded {
            auth.signUpWithEmail(name, email, password)?.toDomain()?.asSuccess()
                ?: DomainError.InvalidInput("Ya existe una cuenta con ese correo").asFailure()
        }

    override suspend fun signInWithSocial(provider: SocialProvider): DomainResult<User> = guarded {
        auth.signInWithSocial(provider.toStorageCode()).toDomain().asSuccess()
    }

    override suspend fun signInWithEmail(email: String, password: String): DomainResult<User> = guarded {
        auth.signInWithEmail(email, password)?.toDomain()?.asSuccess() ?: DomainError.Unauthorized.asFailure()
    }

    override suspend fun signOut(): DomainResult<Unit> = guarded {
        auth.signOut()
        Unit.asSuccess()
    }

    override suspend fun confirmEmail(): DomainResult<User> = guarded {
        auth.confirmEmail()?.toDomain()?.asSuccess() ?: DomainError.Unauthorized.asFailure()
    }

    override suspend fun resendVerificationEmail(): DomainResult<Unit> = guarded {
        if (auth.resendVerificationEmail()) Unit.asSuccess() else DomainError.Unauthorized.asFailure()
    }

    override suspend fun updateProfile(displayName: String, photoUrl: String?): DomainResult<User> = guarded {
        auth.updateProfile(displayName, photoUrl)?.toDomain()?.asSuccess() ?: DomainError.Unauthorized.asFailure()
    }

    override suspend fun deleteAccount(): DomainResult<Unit> = guarded {
        val current = auth.currentUser() ?: return@guarded DomainError.Unauthorized.asFailure()
        if (auth.deleteCurrentAccount(current.toDomain().anonymized().toDto())) {
            Unit.asSuccess()
        } else {
            DomainError.Unauthorized.asFailure()
        }
    }
}

// Deliberate: this boundary is where every real exception gets translated into a DomainError — see
// core:common's DomainResult docs. Same pattern as the other repository implementations, factored
// out here because every method of this repository does it.
@Suppress("TooGenericExceptionCaught")
private inline fun <T> guarded(block: () -> DomainResult<T>): DomainResult<T> = try {
    block()
} catch (e: Exception) {
    DomainError.Unknown(e.message).asFailure()
}
