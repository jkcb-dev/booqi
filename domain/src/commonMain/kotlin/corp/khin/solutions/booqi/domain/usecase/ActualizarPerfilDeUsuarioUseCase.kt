package corp.khin.solutions.booqi.domain.usecase

import corp.khin.solutions.booqi.core.common.DomainResult
import corp.khin.solutions.booqi.core.common.asFailure
import corp.khin.solutions.booqi.domain.model.User
import corp.khin.solutions.booqi.domain.repository.UserRepository

/**
 * Escenario: "El Usuario actualiza su perfil" (docs/domain/identity-flow.md): changes the signed-in
 * user's [name] and [photoUrl]. The name is still required (blank is `InvalidInput`, checked before
 * any I/O); name and photo are trimmed and a blank photo clears it. `Unauthorized` without a
 * session.
 *
 * The new name shows in the user's *next* bookings and reviews. "Existing bookings keep the name
 * saved in them" needs a customer-name snapshot on `Booking` that doesn't exist yet — an open
 * question for the Customer booking flow (#25), see docs/domain/identity-flow.md. This does not
 * touch the user's `ProviderProfile`, which has its own name.
 */
class ActualizarPerfilDeUsuarioUseCase(
    private val users: UserRepository,
) {
    suspend operator fun invoke(name: String, photoUrl: String? = null): DomainResult<User> =
        validateDisplayName(name)?.asFailure()
            ?: users.updateProfile(name.trim(), photoUrl?.trim()?.takeIf { it.isNotEmpty() })
}
