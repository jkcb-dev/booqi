package corp.khin.solutions.booqi.domain.usecase

import corp.khin.solutions.booqi.core.common.DomainError
import corp.khin.solutions.booqi.core.common.DomainResult
import corp.khin.solutions.booqi.core.common.asFailure
import corp.khin.solutions.booqi.domain.model.ProviderProfile
import corp.khin.solutions.booqi.domain.repository.ProviderProfileRepository

/**
 * Escenarios: "El Proveedor completa su perfil" / "El Proveedor intenta completar el perfil sin
 * ubicación" (docs/domain/provider-flow.md § Grupo 1).
 *
 * Modeling decision for the validation failure: "ubicación obligatoria" is a *use-case-level
 * input validation* — it is knowable before any I/O and doesn't depend on repository/network
 * state, unlike [DomainError.NotFound] or [DomainError.NoConnection]. Rather than inventing a
 * second, parallel result/error type just for this use case, this reuses the existing
 * [DomainResult]/[DomainError] closed set — the doc on [DomainError] already describes it as
 * "failures that presentation code is allowed to know about", which is exactly what a form error
 * is. [DomainError.InvalidInput] was added there for this (and future) validation failures. The
 * check runs before the repository is even called, so an invalid submission never reaches
 * persistence.
 */
class CompletarPerfilDeProveedorUseCase(
    private val repository: ProviderProfileRepository,
) {
    suspend operator fun invoke(
        profileId: String,
        name: String,
        photoUrl: String,
        description: String,
        location: String,
    ): DomainResult<ProviderProfile> {
        if (location.isBlank()) {
            return DomainError.InvalidInput("La ubicación es obligatoria").asFailure()
        }
        return repository.completeProfile(
            profileId = profileId,
            name = name,
            photoUrl = photoUrl,
            description = description,
            location = location,
        )
    }
}
