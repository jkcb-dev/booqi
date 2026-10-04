package corp.khin.solutions.booqi.domain.usecase

import corp.khin.solutions.booqi.core.common.DomainResult
import corp.khin.solutions.booqi.core.common.map
import corp.khin.solutions.booqi.domain.model.PublicUser
import corp.khin.solutions.booqi.domain.repository.UserRepository

/**
 * Resolves how a person is shown to *someone else* — the name a Provider sees for
 * `Booking.customerId`, the name next to a review. A deleted account comes back as
 * [corp.khin.solutions.booqi.domain.model.User.DELETED_USER_NAME] with no photo, so history keeps
 * rendering after account deletion. Never exposes the email. `NotFound` for an id with no account
 * (callers fall back to their placeholder).
 */
class ObtenerUsuarioPublicoUseCase(
    private val users: UserRepository,
) {
    suspend operator fun invoke(userId: String): DomainResult<PublicUser> =
        users.getUser(userId).map { it.toPublic() }
}
