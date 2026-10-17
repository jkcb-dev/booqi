package corp.khin.solutions.booqi.domain.usecase

import corp.khin.solutions.booqi.core.common.DomainError
import corp.khin.solutions.booqi.core.common.DomainResult
import corp.khin.solutions.booqi.domain.model.Address
import corp.khin.solutions.booqi.domain.model.User
import corp.khin.solutions.booqi.domain.repository.UserRepository

/**
 * Command `ActualizarDireccion` (docs/domain/customer-flow.md § Grupo 2): the Customer replaces
 * their saved address from their profile — event "Se actualizó la dirección guardada". The new
 * address **replaces** the old one (never a list) for *future* requests; Bookings already requested
 * keep the address copied into them, because nothing here touches any Booking.
 *
 * `InvalidInput` for a blank line or an invalid position, `NotFound` when there is nothing to
 * update yet (that is `AgregarDireccion`), `Unauthorized` without a session. Returns the updated
 * [User].
 */
class ActualizarDireccionUseCase(
    private val users: UserRepository,
) {
    suspend operator fun invoke(address: Address): DomainResult<User> =
        users.saveAddressOfCurrentUser(address) { user ->
            if (user.address == null) DomainError.NotFound else null
        }
}
