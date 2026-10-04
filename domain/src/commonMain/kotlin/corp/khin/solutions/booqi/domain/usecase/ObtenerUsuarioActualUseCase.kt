package corp.khin.solutions.booqi.domain.usecase

import corp.khin.solutions.booqi.core.common.DomainResult
import corp.khin.solutions.booqi.domain.model.User
import corp.khin.solutions.booqi.domain.repository.UserRepository

/**
 * Query `ObtenerUsuarioActual` (docs/domain/identity-flow.md): the signed-in [User], or `null` as a
 * successful answer while the visitor browses without an account (search, service detail and
 * provider profiles are public — a missing session is not an error here).
 */
class ObtenerUsuarioActualUseCase(
    private val users: UserRepository,
) {
    suspend operator fun invoke(): DomainResult<User?> = users.getCurrentUser()
}
