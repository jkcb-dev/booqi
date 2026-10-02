package corp.khin.solutions.booqi.domain.usecase

import corp.khin.solutions.booqi.core.common.DomainResult
import corp.khin.solutions.booqi.domain.model.Service
import corp.khin.solutions.booqi.domain.repository.ServiceRepository

/**
 * Escenario: "El Proveedor deshabilita un Servicio" (docs/domain/provider-flow.md § Grupo 2).
 *
 * Soft-delete only: sets [Service.isActive] to `false` so the Service stops appearing in Customer
 * searches, without removing the row — "las citas ya aceptadas para ese Servicio no se cancelan"
 * and "el historial de citas pasadas conserva la referencia al Servicio" both require
 * `Booking.serviceId` (future ticket) to keep resolving. There is deliberately no "eliminar"
 * counterpart (docs/DOMAIN.md § soft-delete reasoning) — a hard delete would orphan those
 * references, including completed bookings that are part of a Customer's and Provider's history.
 */
class DeshabilitarServicioUseCase(
    private val repository: ServiceRepository,
) {
    suspend operator fun invoke(serviceId: String): DomainResult<Service> =
        repository.disableService(serviceId)
}
