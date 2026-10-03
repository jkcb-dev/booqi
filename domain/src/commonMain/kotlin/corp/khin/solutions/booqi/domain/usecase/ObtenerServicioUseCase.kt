package corp.khin.solutions.booqi.domain.usecase

import corp.khin.solutions.booqi.core.common.DomainResult
import corp.khin.solutions.booqi.domain.model.Service
import corp.khin.solutions.booqi.domain.repository.ServiceRepository

/**
 * Escenarios: "El Proveedor consulta un Servicio para editarlo" / "El Proveedor consulta un
 * Servicio que no existe" (docs/domain/provider-flow.md § Grupo 2). Loads one [Service] by id to
 * preload the edit form (Figma P5). Returns it whether active or disabled; an unknown id
 * propagates `DomainError.NotFound`.
 */
class ObtenerServicioUseCase(
    private val repository: ServiceRepository,
) {
    suspend operator fun invoke(serviceId: String): DomainResult<Service> =
        repository.getService(serviceId)
}
