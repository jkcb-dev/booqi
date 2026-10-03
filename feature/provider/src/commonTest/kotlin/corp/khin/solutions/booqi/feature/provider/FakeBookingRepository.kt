package corp.khin.solutions.booqi.feature.provider

import corp.khin.solutions.booqi.core.common.DomainError
import corp.khin.solutions.booqi.core.common.DomainResult
import corp.khin.solutions.booqi.core.common.asFailure
import corp.khin.solutions.booqi.core.common.asSuccess
import corp.khin.solutions.booqi.domain.model.Booking
import corp.khin.solutions.booqi.domain.model.BookingDraft
import corp.khin.solutions.booqi.domain.model.BookingStatus
import corp.khin.solutions.booqi.domain.repository.BookingRepository
import kotlin.time.Instant

/**
 * Minimal in-memory fake for [BookingRepository], scoped to the booking ViewModels' reducer tests.
 * Same shape as `domain`'s own `FakeBookingRepository` (commonTest sourceSets aren't shared across
 * Gradle modules), kept in sync by hand, plus [listFailure] to simulate a failing load. [seed]
 * stores a ready-made Booking in any state; [writeCount] lets a test assert that a blocked action
 * never reached persistence.
 */
class FakeBookingRepository : BookingRepository {

    private val bookingsById = mutableMapOf<String, Booking>()

    /** When set, the list queries fail with it. */
    var listFailure: DomainError? = null

    /** Number of mutating calls received (create/update). */
    var writeCount = 0
        private set

    fun seed(booking: Booking): Booking = booking.also { bookingsById[it.id] = it }

    fun stored(bookingId: String): Booking = bookingsById.getValue(bookingId)

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
    ): DomainResult<List<Booking>> = listFailure?.asFailure()
        ?: bookingsById.values
            .filter { it.providerId == providerId && (statuses == null || it.status in statuses) }
            .sortedWith(compareBy({ it.scheduledAt }, { it.id }))
            .asSuccess()

    override suspend fun getPendingRequestedAtOrBefore(cutoff: Instant): DomainResult<List<Booking>> =
        bookingsById.values
            .filter { it.status == BookingStatus.REQUESTED && it.requestedAt <= cutoff }
            .sortedWith(compareBy({ it.requestedAt }, { it.id }))
            .asSuccess()

    override suspend fun getRatedBookingsByProvider(providerId: String): DomainResult<List<Booking>> =
        listFailure?.asFailure()
            ?: bookingsById.values
                .filter { it.providerId == providerId && it.rating != null }
                .sortedWith(
                    compareByDescending<Booking> { it.completedAt }
                        .thenByDescending { it.scheduledAt }
                        .thenBy { it.id },
                )
                .asSuccess()
}
