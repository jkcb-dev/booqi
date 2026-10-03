package corp.khin.solutions.booqi.feature.browse

import corp.khin.solutions.booqi.core.common.DomainError
import corp.khin.solutions.booqi.core.common.DomainResult
import corp.khin.solutions.booqi.core.common.asFailure
import corp.khin.solutions.booqi.core.common.asSuccess
import corp.khin.solutions.booqi.domain.model.Booking
import corp.khin.solutions.booqi.domain.model.BookingDraft
import corp.khin.solutions.booqi.domain.model.BookingStatus
import corp.khin.solutions.booqi.domain.model.CatalogEntry
import corp.khin.solutions.booqi.domain.model.DateRange
import corp.khin.solutions.booqi.domain.model.ProviderProfile
import corp.khin.solutions.booqi.domain.model.Service
import corp.khin.solutions.booqi.domain.model.ServiceDetails
import corp.khin.solutions.booqi.domain.repository.BookingRepository
import corp.khin.solutions.booqi.domain.repository.CatalogRepository
import corp.khin.solutions.booqi.domain.repository.ProviderProfileRepository
import corp.khin.solutions.booqi.domain.repository.ServiceRepository
import kotlin.time.Instant

// Minimal in-memory fakes for the Customer's read side, scoped to feature:browse's reducer tests.
// commonTest sourceSets aren't shared across Gradle modules, so — like the other feature modules —
// each module keeps its own, with only what its use cases actually read. The write methods these
// screens never call fail with [NOT_USED] so a stray call can't pass silently.

private val NOT_USED = DomainError.Unknown("not used by feature:browse")

/** Returns [entries] as given (the visibility rules live in `BuscarServiciosUseCase`) and counts
 * [calls], so a test can assert that nothing searched. [failure], when set, fails every read. */
class FakeCatalogRepository(
    var entries: List<CatalogEntry> = emptyList(),
    var failure: DomainError? = null,
) : CatalogRepository {

    var calls = 0
        private set

    override suspend fun getEntries(): DomainResult<List<CatalogEntry>> {
        calls++
        return failure?.asFailure() ?: entries.asSuccess()
    }
}

class FakeServiceRepository(services: List<Service> = emptyList()) : ServiceRepository {

    private val byId = services.associateBy { it.id }.toMutableMap()

    var failure: DomainError? = null

    override suspend fun getService(serviceId: String): DomainResult<Service> =
        failure?.asFailure() ?: byId[serviceId]?.asSuccess() ?: DomainError.NotFound.asFailure()

    override suspend fun getServicesByProvider(providerId: String): DomainResult<List<Service>> =
        failure?.asFailure() ?: byId.values.filter { it.providerId == providerId }.asSuccess()

    override suspend fun addService(providerId: String, details: ServiceDetails) = NOT_USED.asFailure()
    override suspend fun updateService(serviceId: String, details: ServiceDetails) = NOT_USED.asFailure()
    override suspend fun disableService(serviceId: String) = NOT_USED.asFailure()
    override suspend fun enableService(serviceId: String) = NOT_USED.asFailure()
}

class FakeProviderProfileRepository(profiles: List<ProviderProfile> = emptyList()) : ProviderProfileRepository {

    private val byId = profiles.associateBy { it.id }

    override suspend fun getProfile(profileId: String): DomainResult<ProviderProfile> =
        byId[profileId]?.asSuccess() ?: DomainError.NotFound.asFailure()

    override suspend fun activateProviderMode(userId: String) = NOT_USED.asFailure()
    override suspend fun completeProfile(
        profileId: String,
        name: String,
        photoUrl: String,
        description: String,
        location: String,
    ) = NOT_USED.asFailure()

    override suspend fun setPausedRange(profileId: String, pausedRange: DateRange?) = NOT_USED.asFailure()
    override suspend fun updateRating(profileId: String, ratingAverage: Double?, ratingCount: Int) =
        NOT_USED.asFailure()
}

/** Only [getRatedBookingsByProvider] is read (C4's reviews), newest completion first. */
class FakeBookingRepository(private val rated: List<Booking> = emptyList()) : BookingRepository {

    override suspend fun getRatedBookingsByProvider(providerId: String): DomainResult<List<Booking>> =
        rated.filter { it.providerId == providerId && it.rating != null }
            .sortedWith(compareByDescending<Booking> { it.completedAt }.thenBy { it.id })
            .asSuccess()

    override suspend fun createBooking(draft: BookingDraft) = NOT_USED.asFailure()
    override suspend fun getBooking(bookingId: String) = NOT_USED.asFailure()
    override suspend fun updateBooking(booking: Booking) = NOT_USED.asFailure()
    override suspend fun getBookingsByProvider(providerId: String, statuses: Set<BookingStatus>?) =
        NOT_USED.asFailure()

    override suspend fun getPendingRequestedAtOrBefore(cutoff: Instant) = NOT_USED.asFailure()
}
