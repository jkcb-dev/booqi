package corp.khin.solutions.booqi.feature.account

import corp.khin.solutions.booqi.core.common.DomainError
import corp.khin.solutions.booqi.core.common.DomainResult
import corp.khin.solutions.booqi.core.common.asFailure
import corp.khin.solutions.booqi.core.common.asSuccess
import corp.khin.solutions.booqi.domain.model.Booking
import corp.khin.solutions.booqi.domain.model.BookingDraft
import corp.khin.solutions.booqi.domain.model.BookingStatus
import corp.khin.solutions.booqi.domain.model.DateRange
import corp.khin.solutions.booqi.domain.model.ProviderProfile
import corp.khin.solutions.booqi.domain.model.Service
import corp.khin.solutions.booqi.domain.model.ServiceDetails
import corp.khin.solutions.booqi.domain.model.SocialProvider
import corp.khin.solutions.booqi.domain.model.Address
import corp.khin.solutions.booqi.domain.model.User
import corp.khin.solutions.booqi.domain.repository.BookingRepository
import corp.khin.solutions.booqi.domain.repository.ProviderProfileRepository
import corp.khin.solutions.booqi.domain.repository.ServiceRepository
import corp.khin.solutions.booqi.domain.repository.UserRepository
import kotlin.time.Instant

// In-memory fakes scoped to feature:account's reducer tests. commonTest sourceSets aren't shared
// across Gradle modules, so — like the other feature modules — each module keeps its own, with only
// what its use cases actually read. Methods these screens never call fail with [NOT_USED] so a stray
// call can't pass silently.

private val NOT_USED = DomainError.Unknown("not used by feature:account")

/**
 * A working account store: email+password and social accounts, the session, verification emails.
 * [emailLinkFollowed] decides what `confirmEmail` finds: `true` = the User opened the link (the
 * account becomes verified), `false` = they did not (it stays unverified).
 */
class FakeUserRepository : UserRepository {

    private val byId = mutableMapOf<String, User>()
    private val passwords = mutableMapOf<String, String>()
    private var nextId = 1

    var current: User? = null
        private set

    /** Addresses a verification email was sent to, in order (sign-up and re-sends). */
    val verificationEmails = mutableListOf<String>()

    var emailLinkFollowed = true

    /** When set, [getCurrentUser] fails with it. */
    var readFailure: DomainError? = null

    /** Pre-loads [user] (optionally with a sign-in [password]); [signedIn] also starts the session. */
    fun seed(user: User, password: String? = null, signedIn: Boolean = false): User {
        byId[user.id] = user
        if (password != null && user.email != null) passwords[user.email!!] = password
        if (signedIn) current = user
        return user
    }

    private fun create(user: User): User = user.also {
        byId[it.id] = it
        current = it
    }

    override suspend fun getCurrentUser(): DomainResult<User?> = readFailure?.asFailure() ?: current.asSuccess()

    override suspend fun getUser(userId: String): DomainResult<User> =
        byId[userId]?.asSuccess() ?: DomainError.NotFound.asFailure()

    override suspend fun registerWithEmail(name: String, email: String, password: String): DomainResult<User> {
        if (byId.values.any { it.email == email }) {
            return DomainError.InvalidInput("Ya existe una cuenta con ese correo").asFailure()
        }
        passwords[email] = password
        verificationEmails += email
        return create(User(id = "user-${nextId++}", displayName = name, email = email)).asSuccess()
    }

    override suspend fun signInWithSocial(provider: SocialProvider): DomainResult<User> {
        val existing = byId.values.firstOrNull { it.socialProvider == provider }
        val user = existing ?: User(
            id = "user-${nextId++}",
            displayName = "Usuario ${provider.name}",
            email = "usuario@${provider.name.lowercase()}.test",
            isEmailVerified = true,
            socialProvider = provider,
        )
        return create(user).asSuccess()
    }

    override suspend fun signInWithEmail(email: String, password: String): DomainResult<User> {
        val user = byId.values.firstOrNull { it.email == email && !it.isDeleted && passwords[email] == password }
        return user?.let { create(it).asSuccess() } ?: DomainError.Unauthorized.asFailure()
    }

    override suspend fun signOut(): DomainResult<Unit> {
        current = null
        return Unit.asSuccess()
    }

    override suspend fun confirmEmail(): DomainResult<User> {
        val user = current ?: return DomainError.Unauthorized.asFailure()
        if (!emailLinkFollowed) return user.asSuccess()
        return user.copy(isEmailVerified = true).also { byId[it.id] = it; current = it }.asSuccess()
    }

    override suspend fun resendVerificationEmail(): DomainResult<Unit> {
        val email = current?.email ?: return DomainError.Unauthorized.asFailure()
        verificationEmails += email
        return Unit.asSuccess()
    }

    override suspend fun saveAddress(address: Address): DomainResult<User> {
        val user = current ?: return DomainError.Unauthorized.asFailure()
        return user.copy(address = address).also { byId[it.id] = it; current = it }.asSuccess()
    }

    override suspend fun updateProfile(displayName: String, photoUrl: String?): DomainResult<User> {
        val user = current ?: return DomainError.Unauthorized.asFailure()
        return user.copy(displayName = displayName, photoUrl = photoUrl)
            .also { byId[it.id] = it; current = it }.asSuccess()
    }

    override suspend fun deleteAccount(): DomainResult<Unit> {
        val user = current ?: return DomainError.Unauthorized.asFailure()
        user.email?.let { passwords.remove(it) }
        byId[user.id] = user.anonymized()
        current = null
        return Unit.asSuccess()
    }
}

/** Bookings by customer / provider (account deletion reads both); writes are stored as given. */
class FakeBookingRepository : BookingRepository {

    private val bookings = mutableMapOf<String, Booking>()

    fun seed(booking: Booking): Booking = booking.also { bookings[it.id] = it }

    override suspend fun getBookingsByCustomer(
        customerId: String,
        statuses: Set<BookingStatus>?,
    ): DomainResult<List<Booking>> =
        bookings.values.filter { it.customerId == customerId && (statuses == null || it.status in statuses) }
            .sortedBy { it.id }.asSuccess()

    override suspend fun getBookingsByProvider(
        providerId: String,
        statuses: Set<BookingStatus>?,
    ): DomainResult<List<Booking>> =
        bookings.values.filter { it.providerId == providerId && (statuses == null || it.status in statuses) }
            .sortedBy { it.id }.asSuccess()

    override suspend fun updateBooking(booking: Booking): DomainResult<Booking> {
        bookings[booking.id] = booking
        return booking.asSuccess()
    }

    override suspend fun createBooking(draft: BookingDraft) = NOT_USED.asFailure()
    override suspend fun getBooking(bookingId: String) = NOT_USED.asFailure()
    override suspend fun getPendingRequestedAtOrBefore(cutoff: Instant) = NOT_USED.asFailure()
    override suspend fun getRatedBookingsByProvider(providerId: String) = NOT_USED.asFailure()
}

/** Profiles by owner (the Provider entry and account deletion read them). */
class FakeProviderProfileRepository : ProviderProfileRepository {

    private val byId = mutableMapOf<String, ProviderProfile>()

    fun seed(profile: ProviderProfile): ProviderProfile = profile.also { byId[it.id] = it }

    fun stored(profileId: String): ProviderProfile = byId.getValue(profileId)

    override suspend fun findByUserId(userId: String): DomainResult<ProviderProfile?> =
        byId.values.firstOrNull { it.userId == userId }.asSuccess()

    override suspend fun anonymizeProfile(profileId: String): DomainResult<ProviderProfile> {
        val profile = byId[profileId] ?: return DomainError.NotFound.asFailure()
        return profile.copy(isComplete = false, name = null, photoUrl = null, description = null, location = null)
            .also { byId[it.id] = it }.asSuccess()
    }

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

/** Only [getService] is read (naming the Services of the Bookings that block a deletion). */
class FakeServiceRepository(services: List<Service> = emptyList()) : ServiceRepository {

    private val byId = services.associateBy { it.id }

    override suspend fun getService(serviceId: String): DomainResult<Service> =
        byId[serviceId]?.asSuccess() ?: DomainError.NotFound.asFailure()

    override suspend fun getServicesByProvider(providerId: String) = NOT_USED.asFailure()
    override suspend fun addService(providerId: String, details: ServiceDetails) = NOT_USED.asFailure()
    override suspend fun updateService(serviceId: String, details: ServiceDetails) = NOT_USED.asFailure()
    override suspend fun disableService(serviceId: String) = NOT_USED.asFailure()
    override suspend fun enableService(serviceId: String) = NOT_USED.asFailure()
}
