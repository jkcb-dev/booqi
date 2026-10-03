package corp.khin.solutions.booqi.feature.provider

import corp.khin.solutions.booqi.core.common.DomainError
import corp.khin.solutions.booqi.core.common.DomainResult
import corp.khin.solutions.booqi.core.common.asFailure
import corp.khin.solutions.booqi.core.common.asSuccess
import corp.khin.solutions.booqi.domain.model.Service
import corp.khin.solutions.booqi.domain.model.ServiceDetails
import corp.khin.solutions.booqi.domain.repository.ServiceRepository

/**
 * Minimal in-memory fake for [ServiceRepository], scoped to the service ViewModels' reducer
 * tests. Same shape as `domain`'s own `FakeServiceRepository` (commonTest sourceSets aren't shared
 * across Gradle modules), kept in sync by hand, plus [listFailure] to simulate a failing load.
 */
class FakeServiceRepository : ServiceRepository {

    private val servicesById = mutableMapOf<String, Service>()
    private var nextId = 1

    /** When set, [getServicesByProvider] fails with it. */
    var listFailure: DomainError? = null

    val stored: List<Service> get() = servicesById.values.toList()

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

    override suspend fun disableService(serviceId: String): DomainResult<Service> =
        setActive(serviceId, isActive = false)

    override suspend fun enableService(serviceId: String): DomainResult<Service> =
        setActive(serviceId, isActive = true)

    private fun setActive(serviceId: String, isActive: Boolean): DomainResult<Service> {
        val existing = servicesById[serviceId] ?: return DomainError.NotFound.asFailure()
        val updated = existing.copy(isActive = isActive)
        servicesById[serviceId] = updated
        return updated.asSuccess()
    }

    override suspend fun getServicesByProvider(providerId: String): DomainResult<List<Service>> =
        listFailure?.asFailure()
            ?: servicesById.values.filter { it.providerId == providerId }.asSuccess()

    override suspend fun getService(serviceId: String): DomainResult<Service> =
        servicesById[serviceId]?.asSuccess() ?: DomainError.NotFound.asFailure()
}
