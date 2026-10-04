package corp.khin.solutions.booqi.domain.usecase

import corp.khin.solutions.booqi.core.common.DomainError
import corp.khin.solutions.booqi.core.common.DomainResult
import corp.khin.solutions.booqi.core.common.asFailure
import corp.khin.solutions.booqi.core.common.flatMap
import corp.khin.solutions.booqi.domain.model.ProviderProfile
import corp.khin.solutions.booqi.domain.repository.ProviderProfileRepository
import corp.khin.solutions.booqi.domain.repository.UserRepository

/**
 * The user → profile path of the providerId contract (docs/DOMAIN.md): the signed-in user's
 * [ProviderProfile], or `null` as a successful answer when they never activated Provider mode.
 * `Unauthorized` without a session. The Provider screens use `profile.id` as the `providerId` of
 * their Services, Availability and Bookings — never the user id.
 */
class ObtenerMiPerfilDeProveedorUseCase(
    private val users: UserRepository,
    private val profiles: ProviderProfileRepository,
) {
    suspend operator fun invoke(): DomainResult<ProviderProfile?> =
        users.getCurrentUser().flatMap { user ->
            if (user == null) DomainError.Unauthorized.asFailure() else profiles.findByUserId(user.id)
        }
}
