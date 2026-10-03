package corp.khin.solutions.booqi.domain.usecase

import corp.khin.solutions.booqi.core.common.DomainResult
import corp.khin.solutions.booqi.domain.model.Service
import corp.khin.solutions.booqi.domain.repository.ServiceRepository

/**
 * Escenarios: "El Proveedor re-habilita un Servicio deshabilitado" / "El Proveedor re-habilita un
 * Servicio que ya estaba activo" (docs/domain/provider-flow.md § Grupo 2). Symmetric inverse of
 * [DeshabilitarServicioUseCase] — Figma P4 has a single enable/disable toggle per service.
 *
 * Still soft: only flips [Service.isActive] back to `true`, so the Service becomes eligible for
 * Customer search again (that search is a separate, future ticket — nothing here implements it).
 * Idempotent: enabling an already-active Service succeeds and returns it unchanged, so a
 * double-tap on the toggle is harmless. An unknown id propagates `DomainError.NotFound`.
 */
class HabilitarServicioUseCase(
    private val repository: ServiceRepository,
) {
    suspend operator fun invoke(serviceId: String): DomainResult<Service> =
        repository.enableService(serviceId)
}
