package corp.khin.solutions.booqi.domain.usecase

import corp.khin.solutions.booqi.core.common.DomainResult
import corp.khin.solutions.booqi.domain.model.User
import corp.khin.solutions.booqi.domain.repository.SessionRepository

/**
 * Command `VerificarCorreo` (event "Se verificó el correo"): called when the signed-in user has
 * followed the link in the verification email. Returns the [User] with `isEmailVerified = true`,
 * which unlocks booking and Provider mode. Idempotent; `Unauthorized` without a session. The
 * fake backend just flips the flag; the Supabase implementation (#27) refreshes the session and
 * reads the confirmed state. Sending it again is [ReenviarCorreoDeVerificacionUseCase].
 */
class VerificarCorreoUseCase(
    private val session: SessionRepository,
) {
    suspend operator fun invoke(): DomainResult<User> = session.confirmEmail()
}
