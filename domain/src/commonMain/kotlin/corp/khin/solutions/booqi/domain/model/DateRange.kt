package corp.khin.solutions.booqi.domain.model

import kotlinx.datetime.LocalDate

/**
 * An inclusive `[start, end]` span of calendar dates. Value object — equality by value, no
 * identity of its own. Introduced for [ProviderProfile.pausedRange] ("pausar perfil del 10 al 20
 * de agosto", docs/domain/provider-flow.md § Grupo 1); reuse this rather than inventing another
 * shape if a later ticket (e.g. blocked-date ranges in Grupo 3, issue #16) needs the same idea.
 */
data class DateRange(
    val start: LocalDate,
    val end: LocalDate,
) {
    init {
        require(start <= end) { "DateRange start ($start) must not be after end ($end)" }
    }
}
