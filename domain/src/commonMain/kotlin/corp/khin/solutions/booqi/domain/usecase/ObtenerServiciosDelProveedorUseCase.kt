package corp.khin.solutions.booqi.domain.usecase

import corp.khin.solutions.booqi.core.common.DomainResult
import corp.khin.solutions.booqi.domain.model.Service
import corp.khin.solutions.booqi.domain.repository.ServiceRepository

/**
 * Escenario: "El Proveedor ve todos sus Servicios, incluidos los deshabilitados"
 * (docs/domain/provider-flow.md § Grupo 2). Backs the Provider's service list (Figma P4).
 *
 * Returns **all** of [providerId]'s services — active and disabled — oldest first (creation
 * order). This is deliberately *not* the Customer-facing view: Customer search only sees active
 * services and is a separate future use case. A provider with no services gets an empty list, not
 * an error.
 */
class ObtenerServiciosDelProveedorUseCase(
    private val repository: ServiceRepository,
) {
    suspend operator fun invoke(providerId: String): DomainResult<List<Service>> =
        repository.getServicesByProvider(providerId)
}
