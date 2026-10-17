package corp.khin.solutions.booqi.domain.usecase

import corp.khin.solutions.booqi.core.common.DomainError
import corp.khin.solutions.booqi.core.common.DomainResult
import corp.khin.solutions.booqi.core.common.asFailure
import corp.khin.solutions.booqi.core.common.flatMap
import corp.khin.solutions.booqi.domain.model.Address
import corp.khin.solutions.booqi.domain.model.User
import corp.khin.solutions.booqi.domain.repository.UserRepository

/**
 * Rules shared by `AgregarDireccion` and `ActualizarDireccion`, run *before* any repository call
 * (docs/DEVELOPMENT.md § Validation failures). The line is required, and so is a valid map
 * position: the address always comes from a Google Maps search or a map pin (docs/domain/
 * customer-flow.md § Grupo 2), which both carry coordinates.
 */
internal fun validateAddress(address: Address): DomainError.InvalidInput? = when {
    address.line.isBlank() -> DomainError.InvalidInput("La dirección es obligatoria")
    !address.point.isValid -> DomainError.InvalidInput("La ubicación de la dirección no es válida")
    else -> null
}

/** The address as it is stored: the line trimmed, the pin untouched. */
internal fun Address.normalized(): Address = copy(line = line.trim())

/**
 * Validates [address], resolves the signed-in user (`Unauthorized` without a session, or for a
 * deleted account), checks the command's own [precondition] (an error to refuse with, or `null`)
 * and stores the normalized address — the shared body of add and update.
 */
internal suspend fun UserRepository.saveAddressOfCurrentUser(
    address: Address,
    precondition: (User) -> DomainError?,
): DomainResult<User> {
    val invalid = validateAddress(address)
    return if (invalid != null) {
        invalid.asFailure()
    } else {
        getCurrentUser().flatMap { user ->
            val refusal = if (user == null || user.isDeleted) DomainError.Unauthorized else precondition(user)
            refusal?.asFailure() ?: saveAddress(address.normalized())
        }
    }
}
