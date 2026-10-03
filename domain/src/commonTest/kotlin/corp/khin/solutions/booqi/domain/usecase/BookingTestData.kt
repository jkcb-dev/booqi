package corp.khin.solutions.booqi.domain.usecase

import corp.khin.solutions.booqi.core.common.DomainError
import corp.khin.solutions.booqi.core.common.DomainResult
import corp.khin.solutions.booqi.domain.model.Booking
import corp.khin.solutions.booqi.domain.model.BookingStatus
import corp.khin.solutions.booqi.domain.model.Rating
import corp.khin.solutions.booqi.domain.model.Reason
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.datetime.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.time.Duration

/** Shared builders for the Grupo 4 (Booking) tests. */

/** A controllable [Clock]: tests move [now] to cross the 24h boundary. */
class FakeClock(var now: Instant) : Clock {
    override fun now(): Instant = now

    fun advance(by: Duration) {
        now += by
    }
}

/** The moment most tests treat as "the request was made". */
internal val REQUESTED_AT = Instant.parse("2026-10-05T08:00:00Z")

/** A Monday 10:00 appointment, a week after [MONDAY_DATE]. */
internal val APPOINTMENT = LocalDateTime(2026, 10, 12, 10, 0)

@Suppress("LongParameterList")
internal fun booking(
    id: String = "booking-1",
    status: BookingStatus = BookingStatus.REQUESTED,
    providerId: String = "provider-1",
    serviceId: String = "service-1",
    customerId: String = "customer-1",
    scheduledAt: LocalDateTime = APPOINTMENT,
    durationMinutes: Int = 60,
    requestedAt: Instant = REQUESTED_AT,
    reason: Reason? = null,
    rating: Rating? = null,
    completedAt: Instant? = null,
) = Booking(
    id = id,
    providerId = providerId,
    serviceId = serviceId,
    customerId = customerId,
    scheduledAt = scheduledAt,
    durationMinutesSnapshot = durationMinutes,
    priceCentsSnapshot = 3500,
    requestedAt = requestedAt,
    status = status,
    reason = reason,
    rating = rating,
    completedAt = completedAt,
)

/** Asserts a failure carrying [DomainError.InvalidInput] and returns its message. */
internal fun DomainResult<*>.invalidInput(): String {
    assertIs<DomainResult.Failure>(this)
    assertIs<DomainError.InvalidInput>(error)
    return (error as DomainError.InvalidInput).message
}

internal fun DomainResult<*>.assertNotFound() {
    assertIs<DomainResult.Failure>(this)
    assertEquals(DomainError.NotFound, error)
}
