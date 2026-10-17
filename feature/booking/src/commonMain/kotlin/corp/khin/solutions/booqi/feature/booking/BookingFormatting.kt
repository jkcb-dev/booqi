package corp.khin.solutions.booqi.feature.booking

import corp.khin.solutions.booqi.domain.model.Booking
import corp.khin.solutions.booqi.domain.model.ServiceModality
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.Month

private const val CENTS_PER_UNIT = 100
private const val CENTS_DIGITS = 2
private const val TIME_DIGITS = 2

/** `$40.00` — same shape the Provider and Browse screens show a price. */
internal fun formatPrice(priceCents: Int): String =
    "\$${priceCents / CENTS_PER_UNIT}.${(priceCents % CENTS_PER_UNIT).toString().padStart(CENTS_DIGITS, '0')}"

/** `60 min`. */
internal fun formatDuration(minutes: Int): String = "$minutes min"

/** `09:30` — 24-hour clock, as the Provider's schedule shows times. */
internal fun formatTime(time: LocalTime): String =
    "${time.hour.toString().padStart(TIME_DIGITS, '0')}:${time.minute.toString().padStart(TIME_DIGITS, '0')}"

/** `3 de octubre de 2026`. */
internal fun LocalDate.spanishText(): String = "$day de ${month.spanishName()} de $year"

/** `sábado 3 de octubre` — the date navigator's label. */
internal fun LocalDate.spanishLongText(): String = "${dayOfWeek.spanishName()} $day de ${month.spanishName()}"

private fun DayOfWeek.spanishName(): String = when (this) {
    DayOfWeek.MONDAY -> "lunes"
    DayOfWeek.TUESDAY -> "martes"
    DayOfWeek.WEDNESDAY -> "miércoles"
    DayOfWeek.THURSDAY -> "jueves"
    DayOfWeek.FRIDAY -> "viernes"
    DayOfWeek.SATURDAY -> "sábado"
    DayOfWeek.SUNDAY -> "domingo"
}

private fun Month.spanishName(): String = when (this) {
    Month.JANUARY -> "enero"
    Month.FEBRUARY -> "febrero"
    Month.MARCH -> "marzo"
    Month.APRIL -> "abril"
    Month.MAY -> "mayo"
    Month.JUNE -> "junio"
    Month.JULY -> "julio"
    Month.AUGUST -> "agosto"
    Month.SEPTEMBER -> "septiembre"
    Month.OCTOBER -> "octubre"
    Month.NOVEMBER -> "noviembre"
    Month.DECEMBER -> "diciembre"
}

/** Spanish label of the modality badge (same wording as the Provider's list and Browse). */
internal fun ServiceModality.label(): String = when (this) {
    ServiceModality.LOCAL -> "Local"
    ServiceModality.DOMICILIO -> "Domicilio"
    ServiceModality.AMBOS -> "Local & Dom."
}

/** `$12.50` — the price agreed at request time. */
internal fun Booking.priceText(): String = formatPrice(priceCentsSnapshot)

/** `3 de octubre de 2026 · 09:00` — the appointment's date and start time. */
internal fun Booking.whenText(): String = "${scheduledAt.date.spanishText()} · ${formatTime(scheduledAt.time)}"

/** Shown where a Booking's service title can't be resolved. */
internal const val SERVICE_FALLBACK_LABEL = "Servicio"

/** Shown where a Booking's provider name can't be resolved. */
internal const val PROVIDER_FALLBACK_LABEL = "Proveedor"
