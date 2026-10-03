package corp.khin.solutions.booqi.feature.provider

import corp.khin.solutions.booqi.domain.model.Booking
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlin.time.Duration.Companion.minutes

/**
 * True when [Booking]'s appointment has finished at [now]: `scheduledAt` is the Provider's
 * wall-clock start (no timezone feature exists yet, docs/domain/provider-flow.md § Grupo 4), read
 * in [timeZone] — the device's, since that is where the Provider is — plus the duration agreed at
 * request time. The domain deliberately leaves "Completar only after the appointment" to the UI
 * (Figma P10: "Disponible al finalizar la cita").
 */
internal fun Booking.hasEnded(now: Instant, timeZone: TimeZone): Boolean =
    scheduledAt.toInstant(timeZone) + durationMinutesSnapshot.minutes <= now
