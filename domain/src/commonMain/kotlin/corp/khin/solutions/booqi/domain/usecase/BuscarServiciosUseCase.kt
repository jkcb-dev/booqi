package corp.khin.solutions.booqi.domain.usecase

import corp.khin.solutions.booqi.core.common.DomainError
import corp.khin.solutions.booqi.core.common.DomainResult
import corp.khin.solutions.booqi.core.common.asFailure
import corp.khin.solutions.booqi.core.common.map
import corp.khin.solutions.booqi.domain.model.CatalogEntry
import corp.khin.solutions.booqi.domain.model.GeoPoint
import corp.khin.solutions.booqi.domain.model.ServiceSearchCriteria
import corp.khin.solutions.booqi.domain.model.ServiceSearchResult
import corp.khin.solutions.booqi.domain.model.toSummary
import corp.khin.solutions.booqi.domain.repository.CatalogRepository
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

/**
 * Escenarios: "El Cliente busca un Servicio por texto" / "...por categoría (chip)" / "...filtra por
 * distancia" (docs/domain/customer-flow.md § Grupo 1 — `BuscarServicios` + `FiltrarPorDistancia`,
 * one query). Read-only: C1/C2's search over **Services**, never over Providers.
 *
 * **Visibility** — a Service is a candidate only if all hold (docs/DOMAIN.md):
 * - it is active (disabling hides it from Catalog search, soft-delete);
 * - its Provider's profile is complete;
 * - that profile is not paused *today* ([clock] in [timeZone]; the pause range is inclusive on both
 *   ends, so the first and last day of a vacation are hidden, the day before/after are visible).
 *
 * **Filters** (all combined with AND): free text in title or description (every word, ignoring case
 * and accents — "unas" finds "Uñas"), category, and — when [ServiceSearchCriteria.radiusKm] is set —
 * great-circle distance from [ServiceSearchCriteria.location] at most that radius (boundary
 * included). Providers without coordinates are excluded only when a radius is applied.
 *
 * **Order** (deterministic): distance ascending when a location was given (results without a
 * distance last), then title (case/accent-insensitive), then service id. Without a location that is
 * simply title, then id.
 *
 * **Validation** (`InvalidInput`, before any I/O): a radius needs a location and must be positive and
 * finite; a location must be a real coordinate. No results is an empty list, not an error.
 */
class BuscarServiciosUseCase(
    private val catalog: CatalogRepository,
    private val clock: Clock = Clock.System,
    private val timeZone: TimeZone = TimeZone.currentSystemDefault(),
) {
    suspend operator fun invoke(
        criteria: ServiceSearchCriteria = ServiceSearchCriteria(),
    ): DomainResult<List<ServiceSearchResult>> {
        validate(criteria)?.let { return it.asFailure() }
        val today = clock.now().toLocalDateTime(timeZone).date
        val words = criteria.text.orEmpty().foldForSearch().split(WHITESPACE).filter { it.isNotEmpty() }
        return catalog.getEntries().map { entries ->
            entries.asSequence()
                .filter { it.isVisibleOn(today) }
                .filter { criteria.category == null || it.service.category == criteria.category }
                .filter { entry -> words.all { it in entry.searchableText() } }
                .map { it.toResult(criteria.location) }
                .filter { criteria.radiusKm == null || it.isWithin(criteria.radiusKm) }
                .sortedWith(ORDER)
                .toList()
        }
    }

    private fun validate(criteria: ServiceSearchCriteria): DomainError.InvalidInput? {
        val radius = criteria.radiusKm
        return when {
            criteria.location?.isValid == false ->
                DomainError.InvalidInput("La ubicación no es válida")
            radius != null && criteria.location == null ->
                DomainError.InvalidInput("Para filtrar por distancia se necesita la ubicación")
            radius != null && !(radius > 0.0 && radius.isFinite()) ->
                DomainError.InvalidInput("El radio de distancia debe ser mayor que cero")
            else -> null
        }
    }

    private fun CatalogEntry.isVisibleOn(today: LocalDate): Boolean =
        service.isActive && provider.isComplete && provider.pausedRange?.contains(today) != true

    private fun CatalogEntry.searchableText(): String =
        "${service.title} ${service.description}".foldForSearch()

    private fun CatalogEntry.toResult(from: GeoPoint?) = ServiceSearchResult(
        service = service,
        provider = provider.toSummary(),
        distanceKm = from?.let { origin -> provider.coordinates?.let(origin::distanceKmTo) },
    )

    // A radius was validated to come with a location, so "no distance" here means "no coordinates".
    private fun ServiceSearchResult.isWithin(radiusKm: Double): Boolean =
        distanceKm?.let { it <= radiusKm } == true

    private companion object {
        val WHITESPACE = Regex("\\s+")

        val ORDER: Comparator<ServiceSearchResult> = compareBy<ServiceSearchResult>(
            { it.distanceKm ?: Double.MAX_VALUE },
            { it.service.title.foldForSearch() },
            { it.service.id },
        )
    }
}

/** Lowercases and strips the Spanish diacritics so "Uñas"/"unas" and "Técnico"/"tecnico" match. */
internal fun String.foldForSearch(): String = lowercase().map { c ->
    when (c) {
        'á' -> 'a'
        'é' -> 'e'
        'í' -> 'i'
        'ó' -> 'o'
        'ú', 'ü' -> 'u'
        'ñ' -> 'n'
        else -> c
    }
}.joinToString("")
