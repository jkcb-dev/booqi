package corp.khin.solutions.booqi.feature.provider

import corp.khin.solutions.booqi.core.common.DomainError
import corp.khin.solutions.booqi.core.common.DomainResult
import corp.khin.solutions.booqi.core.common.asFailure
import corp.khin.solutions.booqi.core.common.asSuccess
import corp.khin.solutions.booqi.domain.model.ProviderProfile
import corp.khin.solutions.booqi.domain.model.SocialProvider
import corp.khin.solutions.booqi.domain.model.Address
import corp.khin.solutions.booqi.domain.model.User
import corp.khin.solutions.booqi.domain.repository.UserRepository
import corp.khin.solutions.booqi.domain.usecase.ObtenerMiPerfilDeProveedorUseCase

/** The ProviderProfile id the Provider screens' tests act for — `Service.providerId`, never the user id. */
const val TEST_PROVIDER_ID = "provider-profile-test"

/** The id of the signed-in test User, who owns [TEST_PROVIDER_ID]. Different on purpose. */
const val TEST_USER_ID = "user-test"

fun testUser(
    id: String = TEST_USER_ID,
    name: String = "Ana Pérez",
    isEmailVerified: Boolean = true,
) = User(id = id, displayName = name, email = "$id@booqi.test", isEmailVerified = isEmailVerified)

/**
 * Minimal in-memory fake for [UserRepository], scoped to `feature:provider`'s tests (commonTest
 * sourceSets aren't shared across Gradle modules): [current] is the session (`null` = signed out),
 * [users] the accounts [getUser] can find. Everything the Provider screens never call fails.
 */
class FakeUserRepository(
    var current: User? = testUser(),
    users: List<User> = emptyList(),
) : UserRepository {

    private val byId = users.associateBy { it.id }.toMutableMap()

    fun add(user: User) {
        byId[user.id] = user
    }

    override suspend fun getCurrentUser(): DomainResult<User?> = current.asSuccess()

    override suspend fun getUser(userId: String): DomainResult<User> =
        byId[userId]?.asSuccess() ?: DomainError.NotFound.asFailure()

    private fun notUsed() = DomainError.Unknown("not used by feature:provider").asFailure()

    override suspend fun registerWithEmail(name: String, email: String, password: String) = notUsed()
    override suspend fun signInWithSocial(provider: SocialProvider) = notUsed()
    override suspend fun signInWithEmail(email: String, password: String) = notUsed()
    override suspend fun signOut() = notUsed()
    override suspend fun confirmEmail() = notUsed()
    override suspend fun resendVerificationEmail() = notUsed()
    override suspend fun saveAddress(address: Address) = notUsed()
    override suspend fun updateProfile(displayName: String, photoUrl: String?) = notUsed()
    override suspend fun deleteAccount() = notUsed()
}

/** The [CurrentProvider] of a signed-in test User who never activated Provider mode (no profile). */
fun currentProviderWithoutProfile(): CurrentProvider =
    CurrentProvider(ObtenerMiPerfilDeProveedorUseCase(FakeUserRepository(), FakeProviderProfileRepository()))

/**
 * The [CurrentProvider] of a signed-in test User whose ProviderProfile has id [profileId]. Different
 * ids prove a screen reads the profile id, not the user id.
 */
fun testCurrentProvider(
    profileId: String = TEST_PROVIDER_ID,
    users: FakeUserRepository = FakeUserRepository(),
    profiles: FakeProviderProfileRepository = FakeProviderProfileRepository(),
): CurrentProvider {
    val owner = users.current
    if (owner != null) profiles.seed(ProviderProfile(id = profileId, userId = owner.id))
    return CurrentProvider(ObtenerMiPerfilDeProveedorUseCase(users, profiles))
}
