package corp.khin.solutions.booqi.feature.provider

import corp.khin.solutions.booqi.domain.model.ServiceModality

private const val CENTS_PER_UNIT = 100
private const val CENTS_DIGITS = 2
private val PRICE_PATTERN = Regex("""^\d+([.,]\d{1,2})?$""")

/**
 * Parses what a Provider typed into the price field (main currency units, e.g. `12.50` or
 * `12,5`) into the domain's integer `priceCents`. Returns `null` for anything that isn't a
 * strictly positive amount with at most two decimals, or that would overflow an `Int`, so the
 * caller can render a form error instead of crashing.
 */
internal fun parsePriceCents(input: String): Int? {
    val text = input.trim()
    if (!PRICE_PATTERN.matches(text)) return null
    val parts = text.replace(',', '.').split('.')
    val units = parts[0].toLongOrNull()
    val decimals = parts.getOrElse(1) { "" }.padEnd(CENTS_DIGITS, '0').toLong()
    val cents = units?.let { it * CENTS_PER_UNIT + decimals }
    return cents?.takeIf { it in 1..Int.MAX_VALUE }?.toInt()
}

/** Parses the duration field (whole minutes). `null` for non-numeric or non-positive input. */
internal fun parseDurationMinutes(input: String): Int? =
    input.trim().toIntOrNull()?.takeIf { it > 0 }

/** Inverse of [parsePriceCents], used to preload the edit form: `1250` -> `"12.50"`. */
internal fun formatPriceInput(priceCents: Int): String =
    "${priceCents / CENTS_PER_UNIT}.${(priceCents % CENTS_PER_UNIT).toString().padStart(CENTS_DIGITS, '0')}"

/** Spanish label shown on the P4 badge and the P5 modality picker. */
internal fun ServiceModality.label(): String = when (this) {
    ServiceModality.LOCAL -> "Local"
    ServiceModality.DOMICILIO -> "Domicilio"
    ServiceModality.AMBOS -> "Local & Dom."
}
