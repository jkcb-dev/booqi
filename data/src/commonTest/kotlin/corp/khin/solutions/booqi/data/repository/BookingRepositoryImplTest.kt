package corp.khin.solutions.booqi.data.repository

import corp.khin.solutions.booqi.core.common.DomainError
import corp.khin.solutions.booqi.core.common.DomainResult
import corp.khin.solutions.booqi.data.datasource.FakeBookingRemoteDataSource
import corp.khin.solutions.booqi.data.dto.BookingDraftDto
import corp.khin.solutions.booqi.domain.model.Address
import corp.khin.solutions.booqi.domain.model.Booking
import corp.khin.solutions.booqi.domain.model.BookingDraft
import corp.khin.solutions.booqi.domain.model.BookingStatus
import corp.khin.solutions.booqi.domain.model.ProviderReasonCode
import corp.khin.solutions.booqi.domain.model.Rating
import corp.khin.solutions.booqi.domain.model.Reason
import kotlinx.coroutines.test.runTest
import kotlin.time.Instant
import kotlinx.datetime.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.hours

/**
 * Covers what [BookingRepositoryImpl] adds over the datasource: the DTO round trip through the
 * mappers (every status, reason, rating, snapshot and instant), the filtering, the ordering
 * contracts and NotFound. Domain-level BDD scenarios live in `domain`'s use case tests.
 */
class BookingRepositoryImplTest {

    private val repository = BookingRepositoryImpl(FakeBookingRemoteDataSource())

    private val t0 = Instant.parse("2026-10-05T08:00:00Z")

    private fun draft(
        providerId: String = "p1",
        scheduledAt: LocalDateTime = LocalDateTime(2026, 10, 12, 10, 0),
        requestedAt: Instant = t0,
        address: Address? = null,
    ) = BookingDraft(
        providerId = providerId,
        serviceId = "s1",
        customerId = "c1",
        scheduledAt = scheduledAt,
        durationMinutesSnapshot = 60,
        priceCentsSnapshot = 3500,
        requestedAt = requestedAt,
        deliveryAddress = address,
        customerNote = "Llamar al llegar",
    )

    private fun <T> DomainResult<T>.ok(): T = (this as DomainResult.Success<T>).value

    private suspend fun create(d: BookingDraft = draft()) = repository.createBooking(d).ok()

    @Test
    fun `a created booking starts requested with its snapshots and a storage assigned id`() = runTest {
        val created = create(draft(address = Address("Calle 1", 40.5, -3.25)))

        assertEquals("booking-1", created.id)
        assertEquals(BookingStatus.REQUESTED, created.status)
        assertEquals(LocalDateTime(2026, 10, 12, 10, 0), created.scheduledAt)
        assertEquals(60, created.durationMinutesSnapshot)
        assertEquals(3500, created.priceCentsSnapshot)
        assertEquals(Address("Calle 1", 40.5, -3.25), created.deliveryAddress)
        assertEquals("Llamar al llegar", created.customerNote)
        assertEquals(t0, created.requestedAt)
        assertNull(created.rating)
    }

    @Test
    fun `a booking without an address round trips without one`() = runTest {
        assertNull(create().deliveryAddress)
    }

    @Test
    fun `every status reason rating and instant survives an update round trip`() = runTest {
        val created = create()
        val full = created.copy(
            status = BookingStatus.COMPLETED,
            respondedAt = Instant.parse("2026-10-05T09:00:00Z"),
            completedAt = Instant.parse("2026-10-12T11:00:00Z"),
            rating = Rating(4, "Muy bien"),
        )
        repository.updateBooking(full)

        assertEquals(full, repository.getBooking(created.id).ok())

        for (status in BookingStatus.entries) {
            val withStatus = full.copy(status = status)
            repository.updateBooking(withStatus)
            assertEquals(status, repository.getBooking(created.id).ok().status)
        }
        for (code in ProviderReasonCode.entries) {
            val withReason = full.copy(
                status = BookingStatus.REJECTED,
                reason = Reason(code, "nota"),
            )
            repository.updateBooking(withReason)
            assertEquals(withReason, repository.getBooking(created.id).ok())
        }
    }

    @Test
    fun `getting or updating a missing booking is NotFound`() = runTest {
        val missing = Booking.requested("nope", draft())

        assertEquals(DomainError.NotFound, (repository.getBooking("nope") as DomainResult.Failure).error)
        assertEquals(DomainError.NotFound, (repository.updateBooking(missing) as DomainResult.Failure).error)
    }

    @Test
    fun `provider bookings come back by scheduled time then id and only for that provider`() = runTest {
        val late = create(draft(scheduledAt = LocalDateTime(2026, 10, 14, 9, 0)))
        val early = create(draft(scheduledAt = LocalDateTime(2026, 10, 12, 9, 0)))
        val sameTime = create(draft(scheduledAt = LocalDateTime(2026, 10, 12, 9, 0)))
        create(draft(providerId = "other"))

        val result = repository.getBookingsByProvider("p1").ok()

        assertEquals(listOf(early.id, sameTime.id, late.id), result.map { it.id })
    }

    @Test
    fun `provider bookings can be filtered by status and an empty filter matches nothing`() = runTest {
        val requested = create()
        val confirmed = create().let { repository.updateBooking(it.copy(status = BookingStatus.CONFIRMED)).ok() }

        assertEquals(
            listOf(confirmed.id),
            repository.getBookingsByProvider("p1", setOf(BookingStatus.CONFIRMED)).ok().map { it.id },
        )
        assertEquals(
            listOf(requested.id, confirmed.id),
            repository.getBookingsByProvider("p1", setOf(BookingStatus.CONFIRMED, BookingStatus.REQUESTED))
                .ok().map { it.id },
        )
        assertEquals(emptyList(), repository.getBookingsByProvider("p1", emptySet()).ok())
        assertEquals(emptyList(), repository.getBookingsByProvider("nobody").ok())
    }

    @Test
    fun `pending lookup returns requested bookings at or before the cutoff oldest first across providers`() = runTest {
        val newer = create(draft(requestedAt = t0.plusHours(2)))
        val older = create(draft(providerId = "p2", requestedAt = t0))
        val atCutoff = create(draft(requestedAt = t0.plusHours(1)))
        create(draft(requestedAt = t0.plusHours(5))) // after the cutoff
        create().let { repository.updateBooking(it.copy(status = BookingStatus.CONFIRMED)) } // not pending

        val result = repository.getPendingRequestedAtOrBefore(t0.plusHours(2)).ok()

        assertEquals(listOf(older.id, atCutoff.id, newer.id), result.map { it.id })
    }

    @Test
    fun `rated lookup returns only rated bookings of the provider newest completion first`() = runTest {
        fun rated(b: Booking, completed: String, stars: Int) =
            b.copy(status = BookingStatus.COMPLETED, completedAt = Instant.parse(completed), rating = Rating(stars))

        val older = repository.updateBooking(rated(create(), "2026-10-12T11:00:00Z", 3)).ok()
        val newer = repository.updateBooking(rated(create(), "2026-10-19T11:00:00Z", 5)).ok()
        repository.updateBooking(rated(create(draft(providerId = "other")), "2026-10-20T11:00:00Z", 1))
        create() // unrated

        val result = repository.getRatedBookingsByProvider("p1").ok()

        assertEquals(listOf(newer.id, older.id), result.map { it.id })
    }

    @Test
    fun `an unknown stored status surfaces as an Unknown error instead of throwing`() = runTest {
        val dataSource = FakeBookingRemoteDataSource()
        val repo = BookingRepositoryImpl(dataSource)
        val created = dataSource.create(
            BookingDraftDto(
                "p1", "s1", "c1", "2026-10-12T10:00", 60, 3500, null, null, null, null, t0.toString(),
            ),
        )
        dataSource.save(created.copy(status = "Bogus"))

        val result = repo.getBooking(created.id)

        assertIs<DomainResult.Failure>(result)
        assertIs<DomainError.Unknown>(result.error)
    }

    private fun Instant.plusHours(hours: Int): Instant = this + hours.hours
}
