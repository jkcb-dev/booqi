package corp.khin.solutions.booqi.domain.usecase

import corp.khin.solutions.booqi.core.common.DomainError
import corp.khin.solutions.booqi.core.common.DomainResult
import corp.khin.solutions.booqi.core.common.asFailure
import corp.khin.solutions.booqi.domain.model.Service
import corp.khin.solutions.booqi.domain.model.ServiceDetails
import corp.khin.solutions.booqi.domain.repository.ServiceRepository

/**
 * Escenarios: "El Proveedor agrega un nuevo Servicio" / "El Proveedor agrega un Servicio sin
 * foto" (docs/domain/provider-flow.md § Grupo 2).
 *
 * Modeling decision for the validation failure: "foto obligatoria" is a use-case-level input
 * validation — knowable before any I/O, unlike [DomainError.NotFound] or
 * [DomainError.NoConnection]. This reuses the existing [DomainResult]/[DomainError] closed set
 * via [DomainError.InvalidInput] rather than inventing a parallel error type, same reasoning as
 * `CompletarPerfilDeProveedorUseCase` (#12). The check runs before the repository is called, so
 * an invalid submission never reaches persistence.
 *
 * Deliberately does *not* depend on `ProviderProfileRepository` to re-check "perfil completo" —
 * the Gherkin's "Dado que el Proveedor tiene un perfil completo" is story context (when a
 * Provider would realistically reach this screen), not an explicit rejection rule like the
 * "sin foto" scenario's "Cuando... Entonces rechaza". Adding that dependency would be scope creep
 * past this ticket and would reintroduce the ProviderProfile compile-time dependency `Service` is
 * designed to avoid (docs/DOMAIN.md § Aggregate boundaries).
 */
class AgregarServicioUseCase(
    private val repository: ServiceRepository,
) {
    suspend operator fun invoke(providerId: String, details: ServiceDetails): DomainResult<Service> {
        if (details.photoUrl.isBlank()) {
            return DomainError.InvalidInput("La foto es obligatoria").asFailure()
        }
        return repository.addService(providerId, details)
    }
}
