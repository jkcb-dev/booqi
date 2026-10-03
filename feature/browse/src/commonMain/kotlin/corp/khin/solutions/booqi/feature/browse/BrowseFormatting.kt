package corp.khin.solutions.booqi.feature.browse

import corp.khin.solutions.booqi.core.common.DomainError
import corp.khin.solutions.booqi.domain.model.ProviderSummary
import corp.khin.solutions.booqi.domain.model.ServiceCategory
import corp.khin.solutions.booqi.domain.model.ServiceModality
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month

private const val CENTS_PER_UNIT = 100
private const val CENTS_DIGITS = 2
private const val TENTHS = 10
private const val SINGULAR = 1

/** Spanish label of a C1 chip / a result's category. [ServiceCategory.OTRO] has no chip. */
internal fun ServiceCategory.label(): String = when (this) {
    ServiceCategory.BARBERIA -> "Barbería"
    ServiceCategory.UNAS -> "Uñas"
    ServiceCategory.LIMPIEZA -> "Limpieza"
    ServiceCategory.MASAJES -> "Masajes"
    ServiceCategory.TECNICO -> "Técnico"
    ServiceCategory.OTRO -> "Otro"
}

/** Spanish label of the modality badge (same wording as the Provider's P4 list). */
internal fun ServiceModality.label(): String = when (this) {
    ServiceModality.LOCAL -> "Local"
    ServiceModality.DOMICILIO -> "Domicilio"
    ServiceModality.AMBOS -> "Local & Dom."
}

/** `$40.00` — same shape as the Provider screens show a price. */
internal fun formatPrice(priceCents: Int): String =
    "\$${priceCents / CENTS_PER_UNIT}.${(priceCents % CENTS_PER_UNIT).toString().padStart(CENTS_DIGITS, '0')}"

/** `60 min`. */
internal fun formatDuration(minutes: Int): String = "$minutes min"

/** `1.3 km` — one decimal, as a result card shows the distance. */
internal fun formatDistanceKm(distanceKm: Double): String {
    val tenths = (distanceKm * TENTHS).toInt()
    return "${tenths / TENTHS}.${tenths % TENTHS} km"
}

/** `★ 4.5 (12)`, or "Sin calificaciones" for a Provider nobody has rated yet. */
internal fun ProviderSummary.ratingSummary(): String {
    val average = ratingAverage
    if (average == null || ratingCount == 0) return "Sin calificaciones"
    val tenths = (average * TENTHS).toInt()
    return "★ ${tenths / TENTHS}.${tenths % TENTHS} ($ratingCount)"
}

/** The C2 counter: "0 resultados", "1 resultado", "3 resultados". */
internal fun resultCountLabel(count: Int): String =
    if (count == SINGULAR) "1 resultado" else "$count resultados"

/** `25 de diciembre de 2026` — a review's date (same wording as the Provider's P11 preview). */
internal fun LocalDate.spanishText(): String =
    "$day de ${month.spanishName()} de $year"

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

/** User-facing Spanish text for a [DomainError]; [notFound] is what `NotFound` means in context. */
internal fun DomainError.describe(notFound: String): String = when (this) {
    is DomainError.InvalidInput -> message
    is DomainError.Unknown -> message ?: "Ocurrió un error inesperado"
    DomainError.NoConnection -> "Sin conexión"
    DomainError.Timeout -> "La operación tardó demasiado"
    DomainError.NotFound -> notFound
    DomainError.Unauthorized -> "No autorizado"
}
