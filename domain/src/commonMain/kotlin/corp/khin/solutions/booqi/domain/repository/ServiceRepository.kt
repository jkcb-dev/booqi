package corp.khin.solutions.booqi.domain.repository

import corp.khin.solutions.booqi.core.common.DomainResult
import corp.khin.solutions.booqi.domain.model.Service
import corp.khin.solutions.booqi.domain.model.ServiceDetails

/**
 * Domain-owned contract for the `Service` aggregate (docs/domain/provider-flow.md § Grupo 2 —
 * Gestión de Servicios). The implementation (in `data`) decides how a service is
 * sourced/persisted — this interface is the only thing a use case is allowed to know about.
 */
interface ServiceRepository {

    /**
     * Escenario: "El Proveedor agrega un nuevo Servicio". Creates a new [Service] owned by
     * [providerId], active (visible to Customers) by default. Callers must have already validated
     * required fields (see [corp.khin.solutions.booqi.domain.usecase.AgregarServicioUseCase]) —
     * this method assumes valid input and focuses on persistence.
     */
    suspend fun addService(providerId: String, details: ServiceDetails): DomainResult<Service>

    /**
     * Escenario: "El Proveedor edita un Servicio existente". Updates the editable fields of the
     * [Service] identified by [serviceId] going forward — it never touches historical `Booking`
     * snapshots (see [corp.khin.solutions.booqi.domain.usecase.EditarServicioUseCase] KDoc).
     */
    suspend fun updateService(serviceId: String, details: ServiceDetails): DomainResult<Service>

    /**
     * Escenario: "El Proveedor deshabilita un Servicio". Sets [Service.isActive] to `false` on
     * the service identified by [serviceId]. Soft-delete only — never removes the row, so
     * historical `Booking.serviceId` references never dangle.
     */
    suspend fun disableService(serviceId: String): DomainResult<Service>
}
