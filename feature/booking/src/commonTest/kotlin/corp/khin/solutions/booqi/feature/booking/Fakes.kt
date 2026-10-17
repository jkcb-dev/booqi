package corp.khin.solutions.booqi.feature.booking

import corp.khin.solutions.booqi.core.common.DomainError
import corp.khin.solutions.booqi.core.common.DomainResult
import corp.khin.solutions.booqi.core.common.asFailure
import corp.khin.solutions.booqi.core.common.asSuccess
import corp.khin.solutions.booqi.domain.model.Address
import corp.khin.solutions.booqi.domain.model.Availability
import corp.khin.solutions.booqi.domain.model.BlockedPeriod
import corp.khin.solutions.booqi.domain.model.Booking
import corp.khin.solutions.booqi.domain.model.BookingDraft
import corp.khin.solutions.booqi.domain.model.BookingStatus
import corp.khin.solutions.booqi.domain.model.DateRange
import corp.khin.solutions.booqi.domain.model.DayHours
import corp.khin.solutions.booqi.domain.model.ProviderProfile
import corp.khin.solutions.booqi.domain.model.Service
import corp.khin.solutions.booqi.domain.model.ServiceDetails
import corp.khin.solutions.booqi.domain.model.SocialProvider
import corp.khin.solutions.booqi.domain.model.User
import corp.khin.solutions.booqi.domain.repository.AvailabilityRepository
import corp.khin.solutions.booqi.domain.repository.BookingRepository
import corp.khin.solutions.booqi.domain.repository.ProviderProfileRepository
import corp.khin.solutions.booqi.domain.repository.ServiceRepository
import corp.khin.solutions.booqi.domain.repository.UserRepository
import kotlin.time.Instant

// Minimal in-memory fakes, scoped to feature:booking's reducer tests. commonTest sourceSets aren't
// shared across Gradle modules, so — like the other feature modules — each keeps its own, with only
// what its use cases actually read. Calls these screens never make fail with [NOT_USED] so a stray
// call can't pass silently.

private val NOT_USED = DomainError.Unknown("not used by feature:booking")

/** [seed] stores a Booking in any state; [writeCount] lets a test assert a blocked action never
 * reached persistence; [listFailure] fails the list queries. */
class FakeBookingRepository : BookingRepository {

    private val bookingsById = mutableMapOf<String, Booking>()
    var listFailure: DomainError? = null

    var writeCount = 0
        private set

    fun seed(booking: Booking): Booking = booking.also { bookingsById[it.id] = it }

    fun stored(bookingId: String): Booking = bookingsById.getValue(bookingId)

    val all: List<Booking> get() = bookingsById.values.toList()

    override suspend fun createBooking(draft: BookingDraft): DomainResult<Booking> {
        writeCount++
        val created = Booking.requested("booking-created-${bookingsById.size + 1}", draft)
        bookingsById[created.id] = created
        return created.asSuccess()
    }

    override suspend fun getBooking(bookingId: String): DomainResult<Booking> =
        bookingsById[bookingId]?.asSuccess() ?: DomainError.NotFound.asFailure()

    override suspend fun updateBooking(booking: Booking): DomainResult<Booking> {
        writeCount++
        if (booking.id !in bookingsById) return DomainError.NotFound.asFailure()
        bookingsById[booking.id] = booking
        return booking.asSuccess()
    }

    override suspend fun getBookingsByProvider(
        providerId: String,
        statuses: Set<BookingStatus>?,
    ): DomainResult<List<Booking>> = bookingsById.values
        .filter { it.providerId == providerId && (statuses == null || it.status in statuses) }
        .sortedWith(compareBy({ it.scheduledAt }, { it.id }))
        .asSuccess()

    override suspend fun getBookingsByCustomer(
        customerId: String,
        statuses: Set<BookingStatus>?,
    ): DomainResult<List<Booking>> = listFailure?.asFailure()
        ?: bookingsById.values
            .filter { it.customerId == customerId && (statuses == null || it.status in statuses) }
            .sortedWith(compareBy({ it.scheduledAt }, { it.id }))
            .asSuccess()

    override suspend fun getRatedBookingsByProvider(providerId: String): DomainResult<List<Booking>> =
        bookingsById.values
            .filter { it.providerId == providerId && it.rating != null }
            .sortedWith(compareByDescending<Booking> { it.completedAt }.thenBy { it.id })
            .asSuccess()

    override suspend fun getPendingRequestedAtOrBefore(cutoff: Instant) = NOT_USED.asFailure()
}

/** The session: [signedIn] is the current user (`null` = a visitor); [stored] is read back. */
class FakeUserRepository : UserRepository {

    var signedIn: User? = null

    fun signIn(user: User): User = user.also { signedIn = it }

    override suspend fun getCurrentUser(): DomainResult<User?> = signedIn.asSuccess()

    override suspend fun getUser(userId: String) = NOT_USED.asFailure()
    override suspend fun registerWithEmail(name: String, email: String, password: String) = NOT_USED.asFailure()
    override suspend fun signInWithSocial(provider: SocialProvider) = NOT_USED.asFailure()
    override suspend fun signInWithEmail(email: String, password: String) = NOT_USED.asFailure()
    override suspend fun signOut() = NOT_USED.asFailure()
    override suspend fun confirmEmail() = NOT_USED.asFailure()
    override suspend fun resendVerificationEmail() = NOT_USED.asFailure()
    override suspend fun updateProfile(displayName: String, photoUrl: String?) = NOT_USED.asFailure()
    override suspend fun saveAddress(address: Address) = NOT_USED.asFailure()
    override suspend fun deleteAccount() = NOT_USED.asFailure()
}

class FakeAvailabilityRepository : AvailabilityRepository {

    private val byProvider = mutableMapOf<String, Availability>()

    fun seed(availability: Availability) {
        byProvider[availability.providerId] = availability
    }

    override suspend fun getAvailability(providerId: String): DomainResult<Availability> =
        (byProvider[providerId] ?: Availability(providerId)).asSuccess()

    override suspend fun saveWeeklyHours(providerId: String, weeklyHours: List<DayHours>) = NOT_USED.asFailure()
    override suspend fun addBlockedPeriod(providerId: String, period: BlockedPeriod) = NOT_USED.asFailure()
    override suspend fun removeBlockedPeriod(providerId: String, period: BlockedPeriod) = NOT_USED.asFailure()
}

class FakeProviderProfileRepository(profiles: List<ProviderProfile> = emptyList()) : ProviderProfileRepository {

    private val byId = profiles.associateBy { it.id }.toMutableMap()

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
    override suspend fun updateRating(
        profileId: String,
        ratingAverage: Double?,
        ratingCount: Int,
    ): DomainResult<ProviderProfile> {
        val profile = byId[profileId] ?: return DomainError.NotFound.asFailure()
        return profile.copy(ratingAverage = ratingAverage, ratingCount = ratingCount)
            .also { byId[profileId] = it }
            .asSuccess()
    }

    override suspend fun findByUserId(userId: String) = NOT_USED.asFailure()
    override suspend fun anonymizeProfile(profileId: String) = NOT_USED.asFailure()
}

class FakeServiceRepository(services: List<Service> = emptyList()) : ServiceRepository {

    private val byId = services.associateBy { it.id }.toMutableMap()

    override suspend fun getService(serviceId: String): DomainResult<Service> =
        byId[serviceId]?.asSuccess() ?: DomainError.NotFound.asFailure()

    override suspend fun getServicesByProvider(providerId: String): DomainResult<List<Service>> =
        byId.values.filter { it.providerId == providerId }.asSuccess()

    override suspend fun addService(providerId: String, details: ServiceDetails) = NOT_USED.asFailure()
    override suspend fun updateService(serviceId: String, details: ServiceDetails) = NOT_USED.asFailure()
    override suspend fun disableService(serviceId: String) = NOT_USED.asFailure()
    override suspend fun enableService(serviceId: String) = NOT_USED.asFailure()
}
