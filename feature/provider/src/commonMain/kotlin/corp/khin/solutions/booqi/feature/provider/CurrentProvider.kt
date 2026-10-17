package corp.khin.solutions.booqi.feature.provider

import corp.khin.solutions.booqi.core.common.DomainError
import corp.khin.solutions.booqi.core.common.DomainResult
import corp.khin.solutions.booqi.core.common.asFailure
import corp.khin.solutions.booqi.core.common.asSuccess
import corp.khin.solutions.booqi.domain.usecase.ObtenerMiPerfilDeProveedorUseCase

/** What fails when the signed-in User has not activated Provider mode yet (no ProviderProfile). A
 * constant so a form can tell it from a field validation error, both being `InvalidInput`. */
internal val NoProviderProfile = DomainError.InvalidInput("Activá el modo Proveedor para continuar")

/**
 * Whose data the Provider screens show: the **ProviderProfile id** of the signed-in User, which is
 * what `Service`/`Availability`/`Booking`.`providerId` hold (the contract settled in #56 — never the
 * User id). Resolved through [ObtenerMiPerfilDeProveedorUseCase] every time it is needed, not once
 * at construction: our ViewModels outlive a visit (docs/DEVELOPMENT.md), and the account behind them
 * can change between visits (sign out, another account, a profile activated since).
 *
 * Fails with `Unauthorized` without a session, and with `InvalidInput` ([NoProviderProfile]) when
 * the User has no profile yet, so every screen shows an actionable message verbatim.
 */
class CurrentProvider(
    private val obtenerMiPerfil: ObtenerMiPerfilDeProveedorUseCase,
) {
    suspend fun profileId(): DomainResult<String> = when (val result = obtenerMiPerfil()) {
        is DomainResult.Success -> result.value?.id?.asSuccess() ?: NoProviderProfile.asFailure()
        is DomainResult.Failure -> result
    }
}
