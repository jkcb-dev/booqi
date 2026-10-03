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

    /**
     * Escenario: "El Proveedor re-habilita un Servicio deshabilitado". Inverse of [disableService]:
     * sets [Service.isActive] back to `true` on the service identified by [serviceId]. Idempotent —
     * enabling an already-active service succeeds and returns it unchanged. Still soft: it only
     * flips the flag, nothing else about the row changes.
     */
    suspend fun enableService(serviceId: String): DomainResult<Service>

    /**
     * Escenario: "El Proveedor ve todos sus Servicios, incluidos los deshabilitados". Returns
     * **every** [Service] owned by [providerId] — active and disabled alike, because this is the
     * Provider's own management view (Figma P4). The Customer-facing search, which only sees
     * active services, is a different (future) query and must not reuse this one.
     *
     * Ordered by creation (oldest first), so the list does not reshuffle when a service is edited
     * or toggled. A [providerId] with no services yields an empty list, not
     * [corp.khin.solutions.booqi.core.common.DomainError.NotFound] — "no services yet" is a valid
     * state (the Provider's first visit to P4).
     */
    suspend fun getServicesByProvider(providerId: String): DomainResult<List<Service>>

    /**
     * Escenario: "El Proveedor consulta un Servicio para editarlo". Returns the [Service]
     * identified by [serviceId] regardless of [Service.isActive] (a disabled service can still be
     * opened and edited), or [corp.khin.solutions.booqi.core.common.DomainError.NotFound] if it
     * doesn't exist.
     */
    suspend fun getService(serviceId: String): DomainResult<Service>
}
