package corp.khin.solutions.booqi.domain.usecase

import corp.khin.solutions.booqi.core.common.DomainResult
import corp.khin.solutions.booqi.core.common.asFailure
import corp.khin.solutions.booqi.domain.model.Registration
import corp.khin.solutions.booqi.domain.model.User
import corp.khin.solutions.booqi.domain.repository.UserRepository

/**
 * Escenarios: "Un visitante se registra con correo y contraseña" / "...con Google, Facebook o
 * Apple" / "El registro sin nombre se rechaza" (docs/domain/identity-flow.md).
 *
 * - **Email:** the name is required (blank is `InvalidInput` "El nombre es obligatorio"), the email
 *   must look like one and the password must have at least [MIN_PASSWORD_LENGTH] characters — all
 *   checked, in that order, before anything is stored. The account is created, the session started
 *   and a verification email sent; the user is **not** verified yet, so they can sign in but
 *   [RequerirCuentaVerificadaUseCase] will refuse bookings and Provider mode until
 *   [VerificarCorreoUseCase]. Name and email are stored trimmed, the email lowercase; an email that
 *   already has an account is `InvalidInput`.
 * - **Social:** nothing to validate — the provider's result (name and photo prefilled, email
 *   verified) becomes the account and the session starts.
 *
 * The returned [User] is the new account, already signed in.
 */
class RegistrarUsuarioUseCase(
    private val users: UserRepository,
) {
    suspend operator fun invoke(registration: Registration): DomainResult<User> = when (registration) {
        is Registration.WithSocial -> users.signInWithSocial(registration.provider)
        is Registration.WithEmail -> registerWithEmail(registration)
    }

    private suspend fun registerWithEmail(registration: Registration.WithEmail): DomainResult<User> {
        val name = registration.name.trim()
        val email = registration.email.normalizedEmail()
        val invalid = validateDisplayName(name) ?: validateEmail(email) ?: validateNewPassword(registration.password)
        return invalid?.asFailure() ?: users.registerWithEmail(name, email, registration.password)
    }
}
