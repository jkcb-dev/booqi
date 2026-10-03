package corp.khin.solutions.booqi.domain.model

import kotlinx.datetime.LocalDate

/**
 * An inclusive `[start, end]` span of calendar dates. Value object — equality by value, no
 * identity of its own. Introduced for [ProviderProfile.pausedRange] ("pausar perfil del 10 al 20
 * de agosto", docs/domain/provider-flow.md § Grupo 1); also the span slot generation runs over
 * (Grupo 3, #16). Reuse this rather than inventing another date-span shape.
 */
data class DateRange(
    val start: LocalDate,
    val end: LocalDate,
) {
    init {
        require(start <= end) { "DateRange start ($start) must not be after end ($end)" }
    }

    /** True when [date] falls inside this range, both ends included. */
    operator fun contains(date: LocalDate): Boolean = date >= start && date <= end
}
