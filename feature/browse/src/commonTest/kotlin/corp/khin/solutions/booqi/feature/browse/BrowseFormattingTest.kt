package corp.khin.solutions.booqi.feature.browse

import corp.khin.solutions.booqi.core.designsystem.component.RatingReview
import corp.khin.solutions.booqi.domain.model.GeoPoint
import corp.khin.solutions.booqi.domain.model.ProviderReview
import corp.khin.solutions.booqi.domain.model.ServiceCategory
import corp.khin.solutions.booqi.domain.model.ServiceModality
import corp.khin.solutions.booqi.domain.model.toSummary
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Instant

class BrowseFormattingTest {

    @Test
    fun `price shows units and two-digit cents`() {
        assertEquals("\$40.00", formatPrice(4000))
        assertEquals("\$25.50", formatPrice(2550))
        assertEquals("\$0.05", formatPrice(5))
    }

    @Test
    fun `distance shows one decimal`() {
        assertEquals("1.2 km", formatDistanceKm(1.27))
        assertEquals("0.0 km", formatDistanceKm(0.0))
        assertEquals("10.0 km", formatDistanceKm(10.0))
    }

    @Test
    fun `rating summary shows the score and the count or no ratings`() {
        assertEquals("★ 4.5 (2)", CatalogFixture.studio.toSummary().ratingSummary())
        assertEquals("Sin calificaciones", CatalogFixture.studio.copy(ratingAverage = null, ratingCount = 0).toSummary().ratingSummary())
    }

    @Test
    fun `result counter is singular for one`() {
        assertEquals("0 resultados", resultCountLabel(0))
        assertEquals("1 resultado", resultCountLabel(1))
        assertEquals("3 resultados", resultCountLabel(3))
    }

    @Test
    fun `labels match the design copy`() {
        assertEquals(
            listOf("Barbería", "Uñas", "Limpieza", "Masajes", "Técnico"),
            ServiceCategory.filterable.map { it.label() },
        )
        assertEquals(
            listOf("Local", "Domicilio", "Local & Dom."),
            ServiceModality.entries.map { it.label() },
        )
        assertEquals("60 min", formatDuration(60))
    }

    @Test
    fun `spanish date text`() {
        assertEquals("25 de diciembre de 2026", LocalDate(2026, 12, 25).spanishText())
    }

    @Test
    fun `review maps to a rating review dated by its completion`() {
        val review = ProviderReview("b1", 4, "Muy bueno", Instant.parse("2026-09-20T10:00:00Z"))

        assertEquals(
            RatingReview("b1", 4, "Cliente", "Muy bueno", "20 de septiembre de 2026"),
            review.toRatingReview(TimeZone.UTC),
        )
    }

    @Test
    fun `review without comment or date maps to nulls`() {
        val review = ProviderReview("b2", 5, "  ", null)

        val mapped = review.toRatingReview(TimeZone.UTC)

        assertEquals(null, mapped.comment)
        assertEquals(null, mapped.dateLabel)
    }

    /** The doc on TEMPORARY_CUSTOMER_LOCATION promises each distance chip does something visible. */
    @Test
    fun `temporary location sits between the two sample providers`() {
        val studio = checkNotNull(CatalogFixture.studio.coordinates)
        val casa = checkNotNull(CatalogFixture.casa.coordinates)

        assertTrue(TEMPORARY_CUSTOMER_LOCATION.distanceKmTo(studio) in 1.0..2.0)
        assertTrue(TEMPORARY_CUSTOMER_LOCATION.distanceKmTo(casa) in 2.0..5.0)
        assertTrue(TEMPORARY_CUSTOMER_LOCATION.isValid)
        assertEquals(GeoPoint(-34.61, -58.37), TEMPORARY_CUSTOMER_LOCATION)
    }
}
