package corp.khin.solutions.booqi.data.repository

import corp.khin.solutions.booqi.core.common.DomainError
import corp.khin.solutions.booqi.core.common.DomainResult
import corp.khin.solutions.booqi.core.common.asFailure
import corp.khin.solutions.booqi.core.common.asSuccess
import corp.khin.solutions.booqi.data.datasource.ServiceRemoteDataSource
import corp.khin.solutions.booqi.data.dto.ServiceDto
import corp.khin.solutions.booqi.data.mapper.toDomain
import corp.khin.solutions.booqi.data.mapper.toDto
import corp.khin.solutions.booqi.domain.model.Service
import corp.khin.solutions.booqi.domain.model.ServiceDetails
import corp.khin.solutions.booqi.domain.repository.ServiceRepository

class ServiceRepositoryImpl(
    private val remoteDataSource: ServiceRemoteDataSource,
) : ServiceRepository {

    @Suppress("TooGenericExceptionCaught") // deliberate: this boundary is where every real
    // exception (network, serialization, ...) gets translated into a DomainError — see
    // core:common's DomainResult docs. Narrowing this catch would just leak raw exceptions into
    // domain/presentation instead of preventing them. Same pattern as ProviderProfileRepositoryImpl.
    override suspend fun addService(
        providerId: String,
        details: ServiceDetails,
    ): DomainResult<Service> = try {
        remoteDataSource.create(providerId, details.toDto()).toDomain().asSuccess()
    } catch (e: Exception) {
        DomainError.Unknown(e.message).asFailure()
    }

    @Suppress("TooGenericExceptionCaught")
    override suspend fun updateService(
        serviceId: String,
        details: ServiceDetails,
    ): DomainResult<Service> = try {
        val existing = remoteDataSource.findById(serviceId)
            ?: return DomainError.NotFound.asFailure()
        val updated = existing.toDomain().copy(
            title = details.title,
            photoUrl = details.photoUrl,
            description = details.description,
            priceCents = details.priceCents,
            durationMinutes = details.durationMinutes,
            modality = details.modality,
        )
        remoteDataSource.save(updated.toDto()).toDomain().asSuccess()
    } catch (e: Exception) {
        DomainError.Unknown(e.message).asFailure()
    }

    @Suppress("TooGenericExceptionCaught")
    override suspend fun disableService(serviceId: String): DomainResult<Service> = try {
        val existing: ServiceDto = remoteDataSource.findById(serviceId)
            ?: return DomainError.NotFound.asFailure()
        val updated = existing.toDomain().copy(isActive = false)
        remoteDataSource.save(updated.toDto()).toDomain().asSuccess()
    } catch (e: Exception) {
        DomainError.Unknown(e.message).asFailure()
    }
}
