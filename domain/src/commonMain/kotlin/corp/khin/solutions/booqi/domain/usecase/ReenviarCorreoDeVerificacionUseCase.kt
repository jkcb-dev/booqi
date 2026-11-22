package corp.khin.solutions.booqi.domain.usecase

import corp.khin.solutions.booqi.core.common.DomainError
import corp.khin.solutions.booqi.core.common.DomainResult
import corp.khin.solutions.booqi.core.common.asFailure
import corp.khin.solutions.booqi.core.common.flatMap
import corp.khin.solutions.booqi.domain.repository.SessionRepository

/**
 * The "reenviar el correo de verificación" option of the unverified-email scenario
 * (docs/domain/identity-flow.md). `Unauthorized` without a session; an already verified email is
 * `InvalidInput` (nothing to resend) and nothing is sent.
 */
class ReenviarCorreoDeVerificacionUseCase(
    private val session: SessionRepository,
) {
    suspend operator fun invoke(): DomainResult<Unit> =
        session.getCurrentUser().flatMap { user ->
            when {
                user == null -> DomainError.Unauthorized.asFailure()
                user.isEmailVerified -> DomainError.InvalidInput("Tu correo ya está verificado").asFailure()
                else -> session.resendVerificationEmail()
            }
        }
}
