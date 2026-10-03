package corp.khin.solutions.booqi.domain.model

import corp.khin.solutions.booqi.core.common.DomainResult
import corp.khin.solutions.booqi.domain.usecase.REQUESTED_AT
import corp.khin.solutions.booqi.domain.usecase.booking
import corp.khin.solutions.booqi.domain.usecase.invalidInput
import corp.khin.solutions.booqi.domain.usecase.value
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds

/**
 * The Booking state machine (docs/DOMAIN.md, docs/domain/provider-flow.md § Grupo 4), tested as a
 * whole: the transition table against the documented graph, then each transition function against
 * **every** status (valid from its source state only, `InvalidInput` from all others and nothing
 * silently allowed), the 24h window boundary, and the rating rules.
 */
class BookingTest {

    private val deadline = REQUESTED_AT + 24.hours
    private val inWindow = REQUESTED_AT + 1.hours

    // --- The transition table is exactly the documented graph ---

    private val documented = setOf(
        BookingStatus.REQUESTED to BookingStatus.CONFIRMED,
        BookingStatus.REQUESTED to BookingStatus.REJECTED,
        BookingStatus.REQUESTED to BookingStatus.EXPIRED,
        BookingStatus.CONFIRMED to BookingStatus.COMPLETED,
        BookingStatus.CONFIRMED to BookingStatus.CANCELLED_BY_PROVIDER,
        BookingStatus.CONFIRMED to BookingStatus.CANCELLED_BY_CUSTOMER,
    )

    @Test
    fun `canTransitionTo is true for exactly the documented transitions and false for every other pair`() {
        for (from in BookingStatus.entries) {
            for (to in BookingStatus.entries) {
                assertEquals(from to to in documented, from.canTransitionTo(to), "$from -> $to")
            }
        }
    }

    @Test
    fun `terminal states have no way out`() {
        val terminal = BookingStatus.entries.filter { it.isTerminal }.toSet()

        assertEquals(
            setOf(
                BookingStatus.COMPLETED,
                BookingStatus.REJECTED,
                BookingStatus.EXPIRED,
                BookingStatus.CANCELLED_BY_PROVIDER,
                BookingStatus.CANCELLED_BY_CUSTOMER,
            ),
            terminal,
        )
    }

    @Test
    fun `only requested and confirmed bookings hold their slot`() {
        val holding = BookingStatus.entries.filter { it.occupiesSlot }.toSet()

        assertEquals(setOf(BookingStatus.REQUESTED, BookingStatus.CONFIRMED), holding)
    }

    // --- Each transition function: valid from its source only ---

    /** Runs [transition] on a Booking in every status; only [source] may succeed, landing in [target]. */
    private fun assertOnlyFrom(
        source: BookingStatus,
        target: BookingStatus,
        transition: (Booking) -> DomainResult<Booking>,
    ) {
        for (status in BookingStatus.entries) {
            val result = transition(booking(status = status))
            if (status == source) {
                assertEquals(target, result.value().status, "$status -> $target")
            } else {
                result.invalidInput()
            }
        }
    }

    @Test
    fun `confirm moves only a requested booking to confirmed`() =
        assertOnlyFrom(BookingStatus.REQUESTED, BookingStatus.CONFIRMED) { it.confirm(inWindow) }

    @Test
    fun `reject moves only a requested booking to rejected`() =
        assertOnlyFrom(BookingStatus.REQUESTED, BookingStatus.REJECTED) {
            it.reject(ProviderReasonCode.OUTSIDE_SERVICE_AREA, null, inWindow)
        }

    @Test
    fun `expire moves only a requested booking to expired`() =
        assertOnlyFrom(BookingStatus.REQUESTED, BookingStatus.EXPIRED) { it.expire(deadline) }

    @Test
    fun `complete moves only a confirmed booking to completed`() =
        assertOnlyFrom(BookingStatus.CONFIRMED, BookingStatus.COMPLETED) { it.complete(inWindow) }

    @Test
    fun `cancelByProvider moves only a confirmed booking to cancelled by provider`() =
        assertOnlyFrom(BookingStatus.CONFIRMED, BookingStatus.CANCELLED_BY_PROVIDER) {
            it.cancelByProvider(ProviderReasonCode.OTHER, null)
        }

    // --- What each transition records ---

    @Test
    fun `confirm records when the Provider responded`() {
        val confirmed = booking().confirm(inWindow).value()

        assertEquals(inWindow, confirmed.respondedAt)
        assertNull(confirmed.reason)
    }

    @Test
    fun `reject stores the predefined reason and the free text of Otro`() {
        val rejected = booking().reject(ProviderReasonCode.OTHER, "  Estoy enfermo ", inWindow).value()

        assertEquals(Reason(ProviderReasonCode.OTHER, "Estoy enfermo"), rejected.reason)
        assertEquals(inWindow, rejected.respondedAt)
    }

    @Test
    fun `reject without a note or with a blank one stores no note`() {
        assertNull(booking().reject(ProviderReasonCode.OTHER, null, inWindow).value().reason?.note)
        assertNull(booking().reject(ProviderReasonCode.OTHER, "   ", inWindow).value().reason?.note)
    }

    @Test
    fun `complete records the completion instant`() {
        val completed = booking(status = BookingStatus.CONFIRMED).complete(inWindow).value()

        assertEquals(inWindow, completed.completedAt)
    }

    @Test
    fun `cancelByProvider stores the reason`() {
        val cancelled = booking(status = BookingStatus.CONFIRMED)
            .cancelByProvider(ProviderReasonCode.NOT_AVAILABLE_AT_THIS_TIME, null).value()

        assertEquals(Reason(ProviderReasonCode.NOT_AVAILABLE_AT_THIS_TIME), cancelled.reason)
    }

    @Test
    fun `a failed transition returns an error and leaves the original untouched`() {
        val original = booking(status = BookingStatus.COMPLETED)

        original.confirm(inWindow).invalidInput()

        assertEquals(BookingStatus.COMPLETED, original.status)
    }

    // --- The 24h response window (boundary: requestedAt + 24h counts as lapsed) ---

    @Test
    fun `the response deadline is 24 hours after the request`() {
        assertEquals(deadline, booking().responseDeadline)
    }

    @Test
    fun `a request answered one millisecond before the deadline can still be confirmed or rejected`() {
        val justBefore = deadline - 1.milliseconds

        assertEquals(BookingStatus.CONFIRMED, booking().confirm(justBefore).value().status)
        assertEquals(
            BookingStatus.REJECTED,
            booking().reject(ProviderReasonCode.OTHER, null, justBefore).value().status,
        )
    }

    @Test
    fun `a request cannot be confirmed or rejected from the deadline instant on`() {
        booking().confirm(deadline).invalidInput()
        booking().reject(ProviderReasonCode.OTHER, null, deadline).invalidInput()
        booking().confirm(deadline + 5.hours).invalidInput()
    }

    @Test
    fun `expire fails while the window is still open`() {
        val message = booking().expire(deadline - 1.milliseconds).invalidInput()

        assertTrue(message.contains("24 horas"))
    }

    @Test
    fun `expire succeeds exactly at the deadline and after it`() {
        assertEquals(BookingStatus.EXPIRED, booking().expire(deadline).value().status)
        assertEquals(BookingStatus.EXPIRED, booking().expire(deadline + 3.hours).value().status)
    }

    @Test
    fun `a confirmed booking is never overdue however old it is`() {
        assertFalse(booking(status = BookingStatus.CONFIRMED).isResponseOverdue(deadline + 100.hours))
        booking(status = BookingStatus.CONFIRMED).complete(deadline + 100.hours).value()
    }

    // --- Rating rules ---

    @Test
    fun `a completed unrated booking accepts a rating`() {
        val rated = booking(status = BookingStatus.COMPLETED).rate(Rating(4, "Muy bien")).value()

        assertEquals(Rating(4, "Muy bien"), rated.rating)
        assertEquals(BookingStatus.COMPLETED, rated.status)
    }

    @Test
    fun `any status other than completed rejects a rating`() {
        for (status in BookingStatus.entries.filter { it != BookingStatus.COMPLETED }) {
            val message = booking(status = status).rate(Rating(5)).invalidInput()
            assertTrue(message.contains("completada"), "$status: $message")
        }
    }

    @Test
    fun `a booking can be rated only once`() {
        val rated = booking(status = BookingStatus.COMPLETED, rating = Rating(3))

        val message = rated.rate(Rating(5)).invalidInput()

        assertTrue(message.contains("ya fue calificada"))
    }

    @Test
    fun `stars must be between 1 and 5`() {
        val completed = booking(status = BookingStatus.COMPLETED)

        for (stars in listOf(Int.MIN_VALUE, -1, 0, 6, 100)) {
            completed.rate(Rating(stars)).invalidInput()
        }
        for (stars in 1..5) {
            assertEquals(stars, completed.rate(Rating(stars)).value().rating?.stars)
        }
    }

    @Test
    fun `a blank rating comment is stored as no comment`() {
        val completed = booking(status = BookingStatus.COMPLETED)

        assertNull(completed.rate(Rating(5, "   ")).value().rating?.comment)
        assertEquals("Excelente", completed.rate(Rating(5, " Excelente ")).value().rating?.comment)
    }

    // --- Slot overlap and the TimeSlot view ---

    private val tenAm = LocalDateTime(LocalDate(2026, 10, 12), LocalTime(10, 0))

    @Test
    fun `timeSlot is the provider date and start of scheduledAt`() {
        assertEquals(
            TimeSlot("provider-1", LocalDate(2026, 10, 12), LocalTime(10, 0)),
            booking().timeSlot,
        )
    }

    @Test
    fun `a booking occupies a range that overlaps its interval`() {
        val sixtyMinutes = booking(durationMinutes = 60)

        assertTrue(sixtyMinutes.occupies(tenAm, 60))
        assertTrue(sixtyMinutes.occupies(LocalDateTime(LocalDate(2026, 10, 12), LocalTime(10, 30)), 30))
        assertTrue(sixtyMinutes.occupies(LocalDateTime(LocalDate(2026, 10, 12), LocalTime(9, 30)), 60))
    }

    @Test
    fun `back to back ranges do not overlap`() {
        val sixtyMinutes = booking(durationMinutes = 60)

        assertFalse(sixtyMinutes.occupies(LocalDateTime(LocalDate(2026, 10, 12), LocalTime(11, 0)), 60))
        assertFalse(sixtyMinutes.occupies(LocalDateTime(LocalDate(2026, 10, 12), LocalTime(9, 0)), 60))
    }

    @Test
    fun `only active statuses occupy a range`() {
        for (status in BookingStatus.entries) {
            assertEquals(status.occupiesSlot, booking(status = status).occupies(tenAm, 60), "$status")
        }
    }

    @Test
    fun `a range on another day is not occupied`() {
        assertFalse(booking().occupies(LocalDateTime(LocalDate(2026, 10, 13), LocalTime(10, 0)), 60))
    }

    @Test
    fun `requested builds a requested booking from a draft with its snapshots`() {
        val draft = BookingDraft(
            providerId = "p",
            serviceId = "s",
            customerId = "c",
            scheduledAt = tenAm,
            durationMinutesSnapshot = 45,
            priceCentsSnapshot = 2000,
            requestedAt = Instant.parse("2026-10-05T08:00:00Z"),
            deliveryAddress = Address("Calle 1", 1.0, 2.0),
            customerNote = "Timbre roto",
        )

        val created = Booking.requested("booking-9", draft)

        assertEquals(BookingStatus.REQUESTED, created.status)
        assertEquals("booking-9", created.id)
        assertEquals(45, created.durationMinutesSnapshot)
        assertEquals(2000, created.priceCentsSnapshot)
        assertEquals(Address("Calle 1", 1.0, 2.0), created.deliveryAddress)
        assertEquals("Timbre roto", created.customerNote)
        assertNull(created.reason)
        assertNull(created.rating)
    }
}
