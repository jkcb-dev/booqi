package corp.khin.solutions.booqi.domain.usecase

import corp.khin.solutions.booqi.core.common.DomainError
import corp.khin.solutions.booqi.core.common.DomainResult
import corp.khin.solutions.booqi.core.common.asFailure
import corp.khin.solutions.booqi.core.common.flatMap
import corp.khin.solutions.booqi.core.common.map
import corp.khin.solutions.booqi.domain.model.ProviderPublicProfile
import corp.khin.solutions.booqi.domain.model.ProviderReview
import corp.khin.solutions.booqi.domain.model.toSummary
import corp.khin.solutions.booqi.domain.repository.ProviderProfileRepository
import corp.khin.solutions.booqi.domain.repository.ServiceRepository

/**
 * Escenario: "El Cliente ve el perfil completo de un Proveedor" (docs/domain/customer-flow.md
 * § Grupo 1 — `VerPerfilProveedor`, screen C4): the Provider's bio and location, **all their active
 * Services**, their overall rating and the individual reviews, as a [ProviderPublicProfile].
 *
 * The rating aggregate is the profile's `ratingAverage`/`ratingCount` (recomputed by
 * [RecalcularCalificacionDelProveedorUseCase] — not recomputed here), and the reviews come from
 * [ObtenerCalificacionesDelProveedorUseCase] (newest first), reduced to [ProviderReview] so a
 * Booking's private fields don't reach the Customer's screen. Disabled Services are left out; no
 * active Services or no reviews yield empty lists. A missing or incomplete profile is `NotFound`.
 * Like [VerDetalleServicioUseCase], a paused Provider's page stays reachable.
 */
class VerPerfilProveedorUseCase(
    private val profiles: ProviderProfileRepository,
    private val services: ServiceRepository,
    private val ratings: ObtenerCalificacionesDelProveedorUseCase,
) {
    suspend operator fun invoke(providerId: String): DomainResult<ProviderPublicProfile> =
        profiles.getProfile(providerId).flatMap { profile ->
            if (!profile.isComplete) return DomainError.NotFound.asFailure()
            services.getServicesByProvider(providerId).flatMap { all ->
                ratings(providerId).map { rated ->
                    ProviderPublicProfile(
                        provider = profile.toSummary(),
                        services = all.filter { it.isActive },
                        reviews = rated.mapNotNull { booking ->
                            booking.rating?.let {
                                ProviderReview(booking.id, it.stars, it.comment, booking.completedAt)
                            }
                        },
                    )
                }
            }
        }
}
