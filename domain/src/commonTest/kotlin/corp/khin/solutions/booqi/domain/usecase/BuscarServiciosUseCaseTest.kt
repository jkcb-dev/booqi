package corp.khin.solutions.booqi.domain.usecase

import corp.khin.solutions.booqi.core.common.DomainError
import corp.khin.solutions.booqi.core.common.DomainResult
import corp.khin.solutions.booqi.domain.model.CatalogEntry
import corp.khin.solutions.booqi.domain.model.DateRange
import corp.khin.solutions.booqi.domain.model.GeoPoint
import corp.khin.solutions.booqi.domain.model.ProviderProfile
import corp.khin.solutions.booqi.domain.model.Service
import corp.khin.solutions.booqi.domain.model.ServiceCategory
import corp.khin.solutions.booqi.domain.model.ServiceModality
import corp.khin.solutions.booqi.domain.model.ServiceSearchCriteria
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.UtcOffset
import kotlinx.datetime.asTimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.time.Instant

/**
 * Escenarios de docs/domain/customer-flow.md § Grupo 1: búsqueda por texto, por categoría y por
 * distancia, más las reglas de visibilidad (servicio activo, perfil completo, no pausado hoy) y el
 * orden de los resultados. "Hoy" es 2026-10-05 (UTC) salvo que el test diga otra cosa.
 */
class BuscarServiciosUseCaseTest {

    private val customer = GeoPoint(-34.6037, -58.3816)
    private val repository = FakeCatalogRepository()
    private val clock = FakeClock(Instant.parse("2026-10-05T12:00:00Z"))
    private val buscar = BuscarServiciosUseCase(repository, clock, TimeZone.UTC)

    // ~0.0111 degrees of latitude is ~1.235 km, so these offsets give round-ish distances.
    private fun north(km: Double) = GeoPoint(customer.latitude + km / KM_PER_DEGREE, customer.longitude)

    @Suppress("LongParameterList")
    private fun entry(
        id: String,
        title: String = "Servicio $id",
        description: String = "Descripción de $id",
        category: ServiceCategory = ServiceCategory.OTRO,
        active: Boolean = true,
        complete: Boolean = true,
        paused: DateRange? = null,
        coordinates: GeoPoint? = customer,
    ) = CatalogEntry(
        service = Service(
            id = "service-$id",
            providerId = "provider-$id",
            title = title,
            photoUrl = "https://example.com/$id.jpg",
            description = description,
            priceCents = 3500,
            durationMinutes = 60,
            modality = ServiceModality.LOCAL,
            isActive = active,
            category = category,
        ),
        provider = ProviderProfile(
            id = "provider-$id",
            userId = "user-$id",
            name = "Proveedor $id",
            photoUrl = "https://example.com/p-$id.jpg",
            description = "Bio de $id",
            location = "Calle $id",
            isComplete = complete,
            pausedRange = paused,
            ratingAverage = 4.5,
            ratingCount = 8,
            coordinates = coordinates,
        ),
    )

    private suspend fun search(
        text: String? = null,
        category: ServiceCategory? = null,
        location: GeoPoint? = null,
        radiusKm: Double? = null,
    ): List<String> = results(ServiceSearchCriteria(text, category, location, radiusKm)).map { it.service.id }

    private suspend fun results(criteria: ServiceSearchCriteria) = buscar(criteria).value()

    private fun day(day: Int) = LocalDate(2026, 10, day)

    // --- Escenario: El Cliente busca un Servicio por texto -------------------------------------

    @Test
    fun `text matches the title or the description`() = runTest {
        repository.entries = listOf(
            entry("a", title = "Manicure gel"),
            entry("b", title = "Uñas", description = "Incluye manicure clásico"),
            entry("c", title = "Corte de pelo"),
        )

        assertEquals(listOf("service-a", "service-b"), search(text = "manicure"))
    }

    @Test
    fun `text ignores case and accents`() = runTest {
        repository.entries = listOf(entry("a", title = "Uñas esculpidas"), entry("b", title = "Servicio TÉCNICO"))

        assertEquals(listOf("service-a"), search(text = "UNAS"))
        assertEquals(listOf("service-b"), search(text = "tecnico"))
    }

    @Test
    fun `every word of the text must match`() = runTest {
        repository.entries = listOf(
            entry("a", title = "Corte clásico", description = "A domicilio"),
            entry("b", title = "Corte moderno", description = "En el local"),
        )

        assertEquals(listOf("service-a"), search(text = "  corte   domicilio "))
    }

    @Test
    fun `a blank text applies no text filter`() = runTest {
        repository.entries = listOf(entry("a"), entry("b"))

        assertEquals(listOf("service-a", "service-b"), search(text = "   "))
    }

    @Test
    fun `a search with no matches is an empty list and not an error`() = runTest {
        repository.entries = listOf(entry("a", title = "Corte"))

        assertEquals(emptyList(), results(ServiceSearchCriteria(text = "yoga")))
    }

    @Test
    fun `an empty catalog is an empty list`() = runTest {
        assertEquals(emptyList(), results(ServiceSearchCriteria()))
    }

    // --- Escenario: El Cliente busca por categoría (chip) --------------------------------------

    @Test
    fun `a category chip shows only services of that category`() = runTest {
        repository.entries = listOf(
            entry("a", category = ServiceCategory.BARBERIA),
            entry("b", category = ServiceCategory.UNAS),
            entry("c", category = ServiceCategory.BARBERIA),
        )

        assertEquals(listOf("service-a", "service-c"), search(category = ServiceCategory.BARBERIA))
    }

    @Test
    fun `no category chip means Todos and includes every category even OTRO`() = runTest {
        repository.entries = listOf(
            entry("a", category = ServiceCategory.OTRO),
            entry("b", category = ServiceCategory.UNAS),
        )

        assertEquals(listOf("service-a", "service-b"), search(category = null))
    }

    @Test
    fun `text and category combine with AND`() = runTest {
        repository.entries = listOf(
            entry("a", title = "Corte clásico", category = ServiceCategory.BARBERIA),
            entry("b", title = "Corte de uñas", category = ServiceCategory.UNAS),
            entry("c", title = "Barba", category = ServiceCategory.BARBERIA),
        )

        assertEquals(listOf("service-a"), search(text = "corte", category = ServiceCategory.BARBERIA))
    }

    // --- Escenario: El Cliente filtra por distancia --------------------------------------------

    @Test
    fun `a 5 km filter keeps only providers inside the radius`() = runTest {
        repository.entries = listOf(
            entry("near", coordinates = north(2.0)),
            entry("far", coordinates = north(8.0)),
            entry("here", coordinates = customer),
        )

        assertEquals(listOf("service-here", "service-near"), search(location = customer, radiusKm = 5.0))
    }

    @Test
    fun `a provider exactly at the radius is included and just beyond it is not`() = runTest {
        val point = north(3.0)
        val distance = customer.distanceKmTo(point)
        repository.entries = listOf(entry("edge", coordinates = point))

        assertEquals(listOf("service-edge"), search(location = customer, radiusKm = distance))
        assertEquals(emptyList(), search(location = customer, radiusKm = distance - 0.001))
    }

    @Test
    fun `providers without coordinates are excluded only when a radius is applied`() = runTest {
        repository.entries = listOf(entry("known", coordinates = north(1.0)), entry("unknown", coordinates = null))

        assertEquals(listOf("service-known"), search(location = customer, radiusKm = 10.0))
        assertEquals(listOf("service-known", "service-unknown"), search(location = customer))
        assertEquals(listOf("service-known", "service-unknown"), search())
    }

    @Test
    fun `distance combines with text and category`() = runTest {
        repository.entries = listOf(
            entry("ok", title = "Corte", category = ServiceCategory.BARBERIA, coordinates = north(1.0)),
            entry("far", title = "Corte", category = ServiceCategory.BARBERIA, coordinates = north(20.0)),
            entry("other", title = "Corte", category = ServiceCategory.UNAS, coordinates = north(1.0)),
        )

        assertEquals(
            listOf("service-ok"),
            search(text = "corte", category = ServiceCategory.BARBERIA, location = customer, radiusKm = 5.0),
        )
    }

    // --- Orden y tarjeta de resultado ----------------------------------------------------------

    @Test
    fun `results with a location are ordered by distance then title then id with no-distance last`() = runTest {
        repository.entries = listOf(
            entry("z", title = "Zeta", coordinates = north(1.0)),
            entry("x", title = "Alfa", coordinates = north(3.0)),
            entry("w", title = "Beta", coordinates = north(1.0)),
            entry("v", title = "Beta", coordinates = north(1.0)),
            entry("none", title = "Aaa", coordinates = null),
        )

        assertEquals(
            listOf("service-v", "service-w", "service-z", "service-x", "service-none"),
            search(location = customer),
        )
    }

    @Test
    fun `results without a location are ordered by title ignoring case and accents then id`() = runTest {
        repository.entries = listOf(
            entry("1", title = "zumba"),
            entry("2", title = "Árbol"),
            entry("3", title = "alfombra"),
            entry("4", title = "Árbol"),
        )

        assertEquals(listOf("service-3", "service-2", "service-4", "service-1"), search())
    }

    @Test
    fun `a result card carries the provider name rating and the distance`() = runTest {
        repository.entries = listOf(entry("a", coordinates = north(2.0)))

        val card = results(ServiceSearchCriteria(location = customer)).single()

        assertEquals("service-a", card.service.id)
        assertEquals("provider-a", card.provider.id)
        assertEquals("Proveedor a", card.provider.name)
        assertEquals(4.5, card.provider.ratingAverage)
        assertEquals(8, card.provider.ratingCount)
        assertEquals(2.0, assertNotNull(card.distanceKm), absoluteTolerance = 0.01)
    }

    @Test
    fun `without a location a result has no distance`() = runTest {
        repository.entries = listOf(entry("a"))

        assertNull(results(ServiceSearchCriteria()).single().distanceKm)
    }

    // --- Visibilidad: activo, perfil completo, no pausado hoy ----------------------------------

    @Test
    fun `disabled services never appear`() = runTest {
        repository.entries = listOf(entry("on"), entry("off", active = false))

        assertEquals(listOf("service-on"), search())
    }

    @Test
    fun `services of an incomplete profile never appear`() = runTest {
        repository.entries = listOf(entry("done"), entry("draft", complete = false))

        assertEquals(listOf("service-done"), search())
    }

    @Test
    fun `a provider paused today is hidden and shows again as soon as the pause ends`() = runTest {
        repository.entries = listOf(
            entry("mid", paused = DateRange(day(1), day(10))),
            entry("last-day", paused = DateRange(day(1), day(5))),
            entry("first-day", paused = DateRange(day(5), day(9))),
            entry("one-day", paused = DateRange(day(5), day(5))),
            entry("ended", paused = DateRange(day(1), day(4))),
            entry("starts-tomorrow", paused = DateRange(day(6), day(9))),
            entry("never"),
        )

        assertEquals(listOf("service-ended", "service-never", "service-starts-tomorrow"), search())

        clock.now = Instant.parse("2026-10-06T00:00:00Z")
        assertEquals(
            listOf("service-ended", "service-last-day", "service-never", "service-one-day"),
            search(),
        )
    }

    @Test
    fun `today follows the injected time zone and not UTC`() = runTest {
        // 2026-10-06T01:00Z is still Oct 5 (22:00) at UTC-3.
        clock.now = Instant.parse("2026-10-06T01:00:00Z")
        val buenosAires = BuscarServiciosUseCase(repository, clock, UtcOffset(hours = -3).asTimeZone())
        repository.entries = listOf(entry("a", paused = DateRange(day(5), day(5))))

        assertEquals(emptyList(), buenosAires().value().map { it.service.id })
        assertEquals(listOf("service-a"), buscar().value().map { it.service.id })
    }

    // --- Validación -----------------------------------------------------------------------------

    @Test
    fun `a non positive or non finite radius is InvalidInput and never reaches the repository`() = runTest {
        listOf(0.0, -1.0, Double.NaN, Double.POSITIVE_INFINITY).forEach { radius ->
            val result = buscar(ServiceSearchCriteria(location = customer, radiusKm = radius))
            assertIs<DomainError.InvalidInput>((result as DomainResult.Failure).error)
        }
        assertEquals(0, repository.calls)
    }

    @Test
    fun `a radius without a location is InvalidInput`() = runTest {
        val result = buscar(ServiceSearchCriteria(radiusKm = 5.0))

        assertIs<DomainError.InvalidInput>((result as DomainResult.Failure).error)
        assertEquals(0, repository.calls)
    }

    @Test
    fun `an impossible location is InvalidInput`() = runTest {
        val result = buscar(ServiceSearchCriteria(location = GeoPoint(91.0, 0.0), radiusKm = 5.0))

        assertIs<DomainError.InvalidInput>((result as DomainResult.Failure).error)
        assertEquals(0, repository.calls)
    }

    @Test
    fun `a repository failure is propagated unchanged`() = runTest {
        repository.failure = DomainError.NoConnection

        assertEquals(DomainError.NoConnection, (buscar() as DomainResult.Failure).error)
    }

    private companion object {
        const val KM_PER_DEGREE = 111.195
    }
}
