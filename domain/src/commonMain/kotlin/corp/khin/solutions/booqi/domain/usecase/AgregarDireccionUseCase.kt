package corp.khin.solutions.booqi.domain.usecase

import corp.khin.solutions.booqi.core.common.DomainError
import corp.khin.solutions.booqi.core.common.DomainResult
import corp.khin.solutions.booqi.domain.model.Address
import corp.khin.solutions.booqi.domain.model.User
import corp.khin.solutions.booqi.domain.repository.UserRepository

/**
 * Command `AgregarDireccion` (docs/domain/customer-flow.md § Grupo 2): the Customer saves the
 * single address of their profile for the first time — event "Se agregó la dirección del Cliente".
 * [address] comes from a Google Maps search or a map pin (the picker itself is #23/#24); the line
 * is trimmed.
 *
 * `InvalidInput` for a blank line or an invalid position, and when the user **already has** a saved
 * address (there is only one — changing it is `ActualizarDireccion`); `Unauthorized` without a
 * session. Returns the updated [User]. Nothing here touches any Booking: a request takes its own
 * *copy* of the address (#26).
 */
class AgregarDireccionUseCase(
    private val users: UserRepository,
) {
    suspend operator fun invoke(address: Address): DomainResult<User> =
        users.saveAddressOfCurrentUser(address) { user ->
            if (user.address != null) DomainError.InvalidInput(ALREADY_HAS_ADDRESS_MESSAGE) else null
        }

    companion object {
        const val ALREADY_HAS_ADDRESS_MESSAGE = "Ya tienes una dirección guardada; actualízala en su lugar"
    }
}
