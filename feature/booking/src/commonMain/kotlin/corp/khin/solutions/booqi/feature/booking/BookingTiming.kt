package corp.khin.solutions.booqi.feature.booking

import corp.khin.solutions.booqi.domain.model.Booking
import corp.khin.solutions.booqi.domain.model.BookingStatus
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlin.time.Duration
import kotlin.time.Instant

private const val HOURS_PER_DAY = 24
private const val MINUTES_PER_HOUR = 60

/**
 * True while the Customer may still cancel: a CONFIRMED Booking and `now <= start - 3h` — the same
 * inclusive rule `Booking.cancelByCustomer` enforces ("hasta 3 horas antes", docs/domain/
 * customer-flow.md § Grupo 3). `scheduledAt` is the Provider's wall-clock start, read in [timeZone]
 * (the device's), as everywhere else. The screen uses this to disable the button; the domain
 * remains the authority.
 */
internal fun Booking.canBeCancelledByCustomer(now: Instant, timeZone: TimeZone): Boolean =
    status == BookingStatus.CONFIRMED &&
        now <= scheduledAt.toInstant(timeZone) - Booking.CUSTOMER_CANCELLATION_WINDOW

/** Time left until the appointment starts (negative once it has), read in [timeZone]. */
internal fun Booking.timeUntilStart(now: Instant, timeZone: TimeZone): Duration =
    scheduledAt.toInstant(timeZone) - now

/** Time left for the Provider to answer a pending request (negative once the 24h window lapsed). */
internal fun Booking.timeToRespond(now: Instant): Duration = responseDeadline - now

/**
 * `2 d 3 h`, `3 h 20 min` or `15 min`: a textual countdown (C8's 24h one, C9's to the
 * appointment). Under a minute still reads `1 min` so it never shows a bare zero while the
 * deadline has not passed. Callers handle the not-positive case with their own sentence.
 */
internal fun countdownText(remaining: Duration): String {
    val totalMinutes = remaining.inWholeMinutes.coerceAtLeast(1)
    val days = totalMinutes / (HOURS_PER_DAY * MINUTES_PER_HOUR)
    val hours = totalMinutes / MINUTES_PER_HOUR % HOURS_PER_DAY
    val minutes = totalMinutes % MINUTES_PER_HOUR
    return when {
        days > 0 -> "$days d $hours h"
        hours > 0 -> "$hours h $minutes min"
        else -> "$minutes min"
    }
}
