package corp.khin.solutions.booqi.domain.usecase

import corp.khin.solutions.booqi.core.common.DomainResult
import corp.khin.solutions.booqi.domain.repository.UserRepository

/**
 * Escenario: "El Usuario cierra sesión" (docs/domain/identity-flow.md). Ends the session; the app
 * goes back to browsing as a visitor, and [ObtenerUsuarioActualUseCase] answers `null` again, so
 * [RequerirCuentaVerificadaUseCase] refuses to book until the next sign-in. Idempotent.
 */
class CerrarSesionUseCase(
    private val users: UserRepository,
) {
    suspend operator fun invoke(): DomainResult<Unit> = users.signOut()
}
