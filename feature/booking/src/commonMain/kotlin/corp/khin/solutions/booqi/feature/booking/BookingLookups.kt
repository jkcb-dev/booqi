package corp.khin.solutions.booqi.feature.booking

import corp.khin.solutions.booqi.core.common.DomainResult
import corp.khin.solutions.booqi.domain.usecase.ObtenerServicioUseCase
import corp.khin.solutions.booqi.domain.usecase.VerPerfilProveedorUseCase

/**
 * Resolves the labels a [corp.khin.solutions.booqi.domain.model.Booking] lacks: it only holds a
 * `serviceId` and a `providerId`. A plain holder over two existing queries (the ViewModels still
 * talk to the domain only through use cases). Both are a nicety: when one cannot be read the
 * caller shows a generic label instead of failing the screen.
 */
class BookingLookups(
    private val services: ObtenerServicioUseCase,
    private val profiles: VerPerfilProveedorUseCase,
) {
    /** The Service's title in any state (a disabled Service keeps its past Bookings readable). */
    suspend fun serviceTitle(serviceId: String): String? =
        (services(serviceId) as? DomainResult.Success)?.value?.title

    /** Titles of [serviceIds], skipping the ones that cannot be read. */
    suspend fun serviceTitles(serviceIds: Collection<String>): Map<String, String> =
        serviceIds.distinct().mapNotNull { id -> serviceTitle(id)?.let { id to it } }.toMap()

    /** The Provider's display name, or `null` if the profile cannot be read. */
    suspend fun providerName(providerId: String): String? =
        (profiles(providerId) as? DomainResult.Success)?.value?.provider?.name?.takeIf { it.isNotBlank() }
}
