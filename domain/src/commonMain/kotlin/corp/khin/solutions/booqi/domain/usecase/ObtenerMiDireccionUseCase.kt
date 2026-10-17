package corp.khin.solutions.booqi.domain.usecase

import corp.khin.solutions.booqi.core.common.DomainError
import corp.khin.solutions.booqi.core.common.DomainResult
import corp.khin.solutions.booqi.core.common.asFailure
import corp.khin.solutions.booqi.core.common.asSuccess
import corp.khin.solutions.booqi.core.common.flatMap
import corp.khin.solutions.booqi.domain.model.Address
import corp.khin.solutions.booqi.domain.repository.UserRepository

/**
 * Query `ObtenerMiDireccion` (docs/domain/customer-flow.md § Grupo 2): the signed-in user's saved
 * [Address], or `null` as a successful answer when they have not added one yet. This is what a
 * Domicilio request reads to decide "use the saved address without asking again" or "ask for it"
 * (scenarios 1 and 2; wiring it into the request is #26). `Unauthorized` without a session.
 */
class ObtenerMiDireccionUseCase(
    private val users: UserRepository,
) {
    suspend operator fun invoke(): DomainResult<Address?> =
        users.getCurrentUser().flatMap { user ->
            if (user == null || user.isDeleted) DomainError.Unauthorized.asFailure() else user.address.asSuccess()
        }
}
