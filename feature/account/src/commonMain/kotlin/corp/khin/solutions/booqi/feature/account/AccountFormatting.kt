package corp.khin.solutions.booqi.feature.account

import corp.khin.solutions.booqi.domain.model.Booking
import corp.khin.solutions.booqi.domain.model.BookingStatus
import corp.khin.solutions.booqi.domain.model.SocialProvider
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.Month

private val MONTHS = listOf(
    "enero", "febrero", "marzo", "abril", "mayo", "junio",
    "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre",
)

private fun Month.spanishName(): String = MONTHS[ordinal]

private const val TIME_DIGITS = 2

private fun Int.twoDigits(): String = toString().padStart(TIME_DIGITS, '0')

/** `25 de diciembre de 2026 · 09:00` — an appointment's date and start time. */
internal fun LocalDateTime.spanishText(): String =
    "$day de ${month.spanishName()} de $year · ${hour.twoDigits()}:${minute.twoDigits()}"

/** The text of the two statuses that can block an account deletion; any other is never listed. */
internal fun BookingStatus.blockingLabel(): String = when (this) {
    BookingStatus.REQUESTED -> "Pendiente"
    BookingStatus.CONFIRMED -> "Confirmada"
    else -> name
}

/** `Manicure gel · 25 de diciembre de 2026 · 09:00` — one row of the blocking-bookings list. */
internal fun Booking.blockingRowText(serviceTitle: String?): String =
    "${serviceTitle ?: SERVICE_FALLBACK_LABEL} · ${scheduledAt.spanishText()}"

internal const val SERVICE_FALLBACK_LABEL = "Servicio"

/** The button label of each social sign-in provider. */
internal fun SocialProvider.buttonLabel(): String = when (this) {
    SocialProvider.GOOGLE -> "Continuar con Google"
    SocialProvider.FACEBOOK -> "Continuar con Facebook"
    SocialProvider.APPLE -> "Continuar con Apple"
}
