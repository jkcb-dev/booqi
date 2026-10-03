package corp.khin.solutions.booqi.domain.usecase

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
 * Hand-written fake shared by the Grupo 4 use case tests — a minimal in-memory [BookingRepository]
 * good enough to exercise the BDD scenarios in docs/domain/provider-flow.md § Grupo 4 without any
 * real I/O. Honors the documented ordering contracts. [writeCount] lets tests assert that a
 * rejected action never reached persistence; [seed] stores a ready-made Booking in any state.
 */
class FakeBookingRepository : BookingRepository {

    private val bookingsById = mutableMapOf<String, Booking>()
    private var nextId = 1

    /** Number of mutating calls received (create/update). */
    var writeCount = 0
        private set

    /** Stores [booking] as-is (no write counted), for arranging a scenario's "Dado que". */
    fun seed(booking: Booking): Booking = booking.also { bookingsById[it.id] = it }

    fun stored(bookingId: String): Booking = bookingsById.getValue(bookingId)

    override suspend fun createBooking(draft: BookingDraft): DomainResult<Booking> {
        writeCount++
        val created = Booking.requested("booking-${nextId++}", draft)
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

    override suspend fun getPendingRequestedAtOrBefore(cutoff: Instant): DomainResult<List<Booking>> =
        bookingsById.values
            .filter { it.status == BookingStatus.REQUESTED && it.requestedAt <= cutoff }
            .sortedWith(compareBy({ it.requestedAt }, { it.id }))
            .asSuccess()

    override suspend fun getRatedBookingsByProvider(providerId: String): DomainResult<List<Booking>> =
        bookingsById.values
            .filter { it.providerId == providerId && it.rating != null }
            .sortedWith(
                compareByDescending<Booking> { it.completedAt }
                    .thenByDescending { it.scheduledAt }
                    .thenBy { it.id },
            )
            .asSuccess()
}
