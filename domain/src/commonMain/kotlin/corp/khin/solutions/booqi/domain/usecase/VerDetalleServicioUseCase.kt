package corp.khin.solutions.booqi.domain.usecase

import corp.khin.solutions.booqi.core.common.DomainError
import corp.khin.solutions.booqi.core.common.DomainResult
import corp.khin.solutions.booqi.core.common.asFailure
import corp.khin.solutions.booqi.core.common.asSuccess
import corp.khin.solutions.booqi.core.common.flatMap
import corp.khin.solutions.booqi.domain.model.ServiceDetail
import corp.khin.solutions.booqi.domain.model.toSummary
import corp.khin.solutions.booqi.domain.repository.ProviderProfileRepository
import corp.khin.solutions.booqi.domain.repository.ServiceRepository

/**
 * Escenario: "El Cliente ve el detalle de un Servicio" (docs/domain/customer-flow.md § Grupo 1 —
 * `VerDetalleServicio`, screen C3). Title, photo, description, price, duration, modality and the
 * Provider's name/rating, as a [ServiceDetail].
 *
 * What the Customer must not see is `NotFound`: an unknown id, a **disabled** Service (soft-deleted
 * from the Customer's point of view), a missing profile (the join is `Service.providerId ==
 * ProviderProfile.id`, with no fallback) or a profile that is not complete. A *paused* Provider is
 * only hidden from search: the detail stays reachable (a Customer who had it open, a past Booking),
 * and booking is blocked anyway because a paused profile has no available TimeSlots.
 *
 * The "horarios disponibles" half of the scenario is the existing
 * [ObtenerTimeSlotsDisponiblesUseCase] (providerId + the Service's duration + a date range); this
 * use case doesn't duplicate it.
 */
class VerDetalleServicioUseCase(
    private val services: ServiceRepository,
    private val profiles: ProviderProfileRepository,
) {
    suspend operator fun invoke(serviceId: String): DomainResult<ServiceDetail> =
        services.getService(serviceId).flatMap { service ->
            if (!service.isActive) return DomainError.NotFound.asFailure()
            profiles.getProfile(service.providerId).flatMap { profile ->
                if (profile.isComplete) {
                    ServiceDetail(service, profile.toSummary()).asSuccess()
                } else {
                    DomainError.NotFound.asFailure()
                }
            }
        }
}
