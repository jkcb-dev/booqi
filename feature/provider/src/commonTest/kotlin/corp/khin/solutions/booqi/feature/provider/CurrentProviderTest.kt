package corp.khin.solutions.booqi.feature.provider

import corp.khin.solutions.booqi.core.common.DomainError
import corp.khin.solutions.booqi.core.common.DomainResult
import corp.khin.solutions.booqi.domain.model.ProviderProfile
import corp.khin.solutions.booqi.domain.usecase.ObtenerMiPerfilDeProveedorUseCase
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * The providerId contract of #56/#58: the Provider screens act for the **ProviderProfile id** of the
 * signed-in User (what `Service.providerId` holds), never for the user id.
 */
class CurrentProviderTest {

    @Test
    fun `resolves the id of the profile and not the id of the user`() = runTest {
        val provider = testCurrentProvider(profileId = "provider-profile-77")

        assertEquals(DomainResult.Success("provider-profile-77"), provider.profileId())
    }

    @Test
    fun `follows the account when the session changes between two calls`() = runTest {
        val users = FakeUserRepository(current = testUser(id = "user-a"))
        val profiles = FakeProviderProfileRepository()
        val provider = testCurrentProvider(profileId = "profile-a", users = users, profiles = profiles)
        assertEquals(DomainResult.Success("profile-a"), provider.profileId())

        users.current = testUser(id = "user-b")
        profiles.seed(ProviderProfile(id = "profile-b", userId = "user-b"))

        assertEquals(DomainResult.Success("profile-b"), provider.profileId())
    }

    @Test
    fun `a user who never activated Provider mode has no provider id`() = runTest {
        val users = FakeUserRepository(current = testUser())
        val provider = CurrentProvider(ObtenerMiPerfilDeProveedorUseCase(users, FakeProviderProfileRepository()))

        assertEquals(DomainResult.Failure(NoProviderProfile), provider.profileId())
    }

    @Test
    fun `without a session there is no provider id`() = runTest {
        val provider = testCurrentProvider(users = FakeUserRepository(current = null))

        assertEquals(DomainResult.Failure(DomainError.Unauthorized), provider.profileId())
    }
}
