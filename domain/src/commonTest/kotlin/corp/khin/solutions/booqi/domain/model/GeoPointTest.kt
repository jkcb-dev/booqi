package corp.khin.solutions.booqi.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GeoPointTest {

    @Test
    fun `the distance to itself is zero`() {
        val p = GeoPoint(-34.6037, -58.3816)

        assertEquals(0.0, p.distanceKmTo(p))
    }

    @Test
    fun `one degree of latitude is about 111 km and the distance is symmetric`() {
        val a = GeoPoint(0.0, 0.0)
        val b = GeoPoint(1.0, 0.0)

        assertEquals(111.19, a.distanceKmTo(b), absoluteTolerance = 0.05)
        assertEquals(a.distanceKmTo(b), b.distanceKmTo(a), absoluteTolerance = 1e-9)
    }

    @Test
    fun `Buenos Aires to Montevideo is about 205 km`() {
        val buenosAires = GeoPoint(-34.6037, -58.3816)
        val montevideo = GeoPoint(-34.9011, -56.1645)

        assertEquals(205.0, buenosAires.distanceKmTo(montevideo), absoluteTolerance = 5.0)
    }

    @Test
    fun `antipodal points do not blow up the math`() {
        assertEquals(20015.0, GeoPoint(0.0, 0.0).distanceKmTo(GeoPoint(0.0, 180.0)), absoluteTolerance = 5.0)
    }

    @Test
    fun `coordinates are valid inside their ranges only`() {
        assertTrue(GeoPoint(90.0, 180.0).isValid)
        assertTrue(GeoPoint(-90.0, -180.0).isValid)
        assertFalse(GeoPoint(90.1, 0.0).isValid)
        assertFalse(GeoPoint(0.0, -180.1).isValid)
        assertFalse(GeoPoint(Double.NaN, 0.0).isValid)
    }

    @Test
    fun `an address exposes its pin as a GeoPoint`() {
        assertEquals(GeoPoint(-34.6, -58.4), Address("Av. Corrientes 1234", -34.6, -58.4).point)
    }

    @Test
    fun `filterable categories are the chip list without OTRO`() {
        assertEquals(
            listOf(
                ServiceCategory.BARBERIA,
                ServiceCategory.UNAS,
                ServiceCategory.LIMPIEZA,
                ServiceCategory.MASAJES,
                ServiceCategory.TECNICO,
            ),
            ServiceCategory.filterable,
        )
        assertFalse(ServiceCategory.OTRO in ServiceCategory.filterable)
    }
}
