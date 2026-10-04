package corp.khin.solutions.booqi.domain.usecase

import corp.khin.solutions.booqi.core.common.DomainError
import corp.khin.solutions.booqi.core.common.DomainResult
import corp.khin.solutions.booqi.core.common.asFailure
import corp.khin.solutions.booqi.core.common.asSuccess
import corp.khin.solutions.booqi.core.common.flatMap
import corp.khin.solutions.booqi.domain.model.User
import corp.khin.solutions.booqi.domain.repository.UserRepository

/**
 * The access guard (docs/domain/identity-flow.md): **requesting a booking and activating Provider
 * mode need a signed-in user with a verified email**; browsing never does. Call it before those two
 * actions and use the returned [User] (`id` is the `customerId` / the `userId`).
 *
 * Failures, in the order they are checked — the caller maps them to a screen:
 * - [DomainError.Unauthorized]: nobody is signed in → ask them to register or sign in (and come
 *   back to what they were doing);
 * - [DomainError.InvalidInput] with [EMAIL_NOT_VERIFIED_MESSAGE]: signed in but the email is not
 *   verified → tell them to verify, offering [ReenviarCorreoDeVerificacionUseCase].
 *
 * Those two are unambiguous here (this use case returns no other `InvalidInput`/`Unauthorized`
 * meaning). A dedicated `DomainError` variant would be more explicit, but adding one breaks the
 * exhaustive `when (error)` in `feature:*` — left to the UI sub-ticket; swapping it in is a
 * one-line change in [guard].
 *
 * Not wired into [SolicitarReservaUseCase] / [ActivarModoProveedorUseCase] on purpose (it would
 * change their signatures and force `feature:*` changes): the UI sub-ticket applies it in front of
 * both.
 */
class RequerirCuentaVerificadaUseCase(
    private val users: UserRepository,
) {
    suspend operator fun invoke(): DomainResult<User> = users.getCurrentUser().flatMap { guard(it) }

    companion object {
        const val EMAIL_NOT_VERIFIED_MESSAGE = "Verificá tu correo para continuar"

        /** The pure rule: [user] is the session's user, `null` when there is none. */
        fun guard(user: User?): DomainResult<User> = when {
            user == null || user.isDeleted -> DomainError.Unauthorized.asFailure()
            !user.isEmailVerified -> DomainError.InvalidInput(EMAIL_NOT_VERIFIED_MESSAGE).asFailure()
            else -> user.asSuccess()
        }
    }
}
