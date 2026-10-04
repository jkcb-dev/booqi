package corp.khin.solutions.booqi.domain.usecase

import corp.khin.solutions.booqi.core.common.DomainError
import corp.khin.solutions.booqi.core.common.DomainResult
import corp.khin.solutions.booqi.core.common.asFailure
import corp.khin.solutions.booqi.domain.model.Credentials
import corp.khin.solutions.booqi.domain.model.User
import corp.khin.solutions.booqi.domain.repository.UserRepository

/**
 * Command `IniciarSesion` (docs/domain/identity-flow.md, event "Se inició sesión"): starts the
 * session of an existing account — also what "return to the booking I was making" needs after a
 * visitor is asked to sign in. An unverified email account *can* sign in (only booking and Provider
 * mode need verification).
 *
 * An empty email or password is `InvalidInput`; wrong credentials are `Unauthorized` (deliberately
 * the same answer for an unknown email and a wrong password). Social sign-in has no input to check.
 */
class IniciarSesionUseCase(
    private val users: UserRepository,
) {
    suspend operator fun invoke(credentials: Credentials): DomainResult<User> = when (credentials) {
        is Credentials.WithSocial -> users.signInWithSocial(credentials.provider)
        is Credentials.WithEmail -> when {
            credentials.email.isBlank() -> DomainError.InvalidInput("El correo es obligatorio").asFailure()
            credentials.password.isEmpty() -> DomainError.InvalidInput("La contraseña es obligatoria").asFailure()
            else -> users.signInWithEmail(credentials.email.normalizedEmail(), credentials.password)
        }
    }
}
