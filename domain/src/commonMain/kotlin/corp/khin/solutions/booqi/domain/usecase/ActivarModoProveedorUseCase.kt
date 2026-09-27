package corp.khin.solutions.booqi.domain.usecase

import corp.khin.solutions.booqi.core.common.DomainResult
import corp.khin.solutions.booqi.domain.model.ProviderProfile
import corp.khin.solutions.booqi.domain.repository.ProviderProfileRepository

/**
 * Escenario: "Un usuario activa el modo Proveedor" (docs/domain/provider-flow.md § Grupo 1).
 *
 * Creates an empty [ProviderProfile] tied to the User's account so they can access Provider
 * management screens. Completing the profile is a separate step
 * ([CompletarPerfilDeProveedorUseCase]) — one class, one action (SRP).
 */
class ActivarModoProveedorUseCase(
    private val repository: ProviderProfileRepository,
) {
    suspend operator fun invoke(userId: String): DomainResult<ProviderProfile> =
        repository.activateProviderMode(userId)
}
