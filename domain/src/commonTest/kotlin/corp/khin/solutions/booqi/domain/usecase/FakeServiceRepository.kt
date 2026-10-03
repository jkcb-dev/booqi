package corp.khin.solutions.booqi.domain.usecase

import corp.khin.solutions.booqi.core.common.DomainError
import corp.khin.solutions.booqi.core.common.DomainResult
import corp.khin.solutions.booqi.core.common.asFailure
import corp.khin.solutions.booqi.core.common.asSuccess
import corp.khin.solutions.booqi.domain.model.Service
import corp.khin.solutions.booqi.domain.model.ServiceDetails
import corp.khin.solutions.booqi.domain.repository.ServiceRepository

/**
 * Hand-written fake shared by the Grupo 2 use case tests (Agregar/Editar/Deshabilitar/Habilitar/Obtener) — a
 * minimal, in-memory implementation of [ServiceRepository] good enough to exercise the BDD
 * scenarios in docs/domain/provider-flow.md § Grupo 2 without any real I/O. Not the same class as
 * `data`'s `FakeServiceRemoteDataSource` — that one fakes the datasource boundary for the app;
 * this one fakes the repository boundary for domain unit tests.
 */
class FakeServiceRepository : ServiceRepository {

    private val servicesById = mutableMapOf<String, Service>()
    private var nextId = 1

    override suspend fun addService(providerId: String, details: ServiceDetails): DomainResult<Service> {
        val service = Service(
            id = "service-${nextId++}",
            providerId = providerId,
            title = details.title,
            photoUrl = details.photoUrl,
            description = details.description,
            priceCents = details.priceCents,
            durationMinutes = details.durationMinutes,
            modality = details.modality,
            isActive = true,
        )
        servicesById[service.id] = service
        return service.asSuccess()
    }

    override suspend fun updateService(serviceId: String, details: ServiceDetails): DomainResult<Service> {
        val existing = servicesById[serviceId] ?: return DomainError.NotFound.asFailure()
        val updated = existing.copy(
            title = details.title,
            photoUrl = details.photoUrl,
            description = details.description,
            priceCents = details.priceCents,
            durationMinutes = details.durationMinutes,
            modality = details.modality,
        )
        servicesById[serviceId] = updated
        return updated.asSuccess()
    }

    override suspend fun disableService(serviceId: String): DomainResult<Service> {
        val existing = servicesById[serviceId] ?: return DomainError.NotFound.asFailure()
        val updated = existing.copy(isActive = false)
        servicesById[serviceId] = updated
        return updated.asSuccess()
    }

    override suspend fun enableService(serviceId: String): DomainResult<Service> {
        val existing = servicesById[serviceId] ?: return DomainError.NotFound.asFailure()
        val updated = existing.copy(isActive = true)
        servicesById[serviceId] = updated
        return updated.asSuccess()
    }

    // servicesById is insertion-ordered and re-assigning an existing key keeps its position, so
    // this is creation order — same contract as the real repository.
    override suspend fun getServicesByProvider(providerId: String): DomainResult<List<Service>> =
        servicesById.values.filter { it.providerId == providerId }.asSuccess()

    override suspend fun getService(serviceId: String): DomainResult<Service> =
        servicesById[serviceId]?.asSuccess() ?: DomainError.NotFound.asFailure()
}
