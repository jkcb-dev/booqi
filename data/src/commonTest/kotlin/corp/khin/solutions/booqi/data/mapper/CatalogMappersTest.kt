package corp.khin.solutions.booqi.data.mapper

import corp.khin.solutions.booqi.data.dto.ProviderProfileDto
import corp.khin.solutions.booqi.data.dto.ServiceDto
import corp.khin.solutions.booqi.domain.model.GeoPoint
import corp.khin.solutions.booqi.domain.model.ServiceCategory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CatalogMappersTest {

    private val profileDto = ProviderProfileDto(
        id = "p1",
        userId = "u1",
        name = "Jane",
        photoUrl = null,
        description = null,
        location = "Calle 1",
        isComplete = true,
        pausedRangeStart = null,
        pausedRangeEnd = null,
        ratingAverage = null,
        ratingCount = 0,
        locationLat = -34.6,
        locationLng = -58.4,
    )

    private val serviceDto = ServiceDto("s1", "p1", "Corte", "u", "d", 1000, 30, "local", true, "barberia")

    @Test
    fun `profile coordinates survive a round trip`() {
        val domain = profileDto.toDomain()

        assertEquals(GeoPoint(-34.6, -58.4), domain.coordinates)
        assertEquals(profileDto, domain.toDto())
    }

    @Test
    fun `a profile with only one of the two coordinates has none`() {
        assertNull(profileDto.copy(locationLng = null).toDomain().coordinates)
        assertNull(profileDto.copy(locationLat = null, locationLng = null).toDomain().coordinates)
    }

    @Test
    fun `every category round trips through its column value`() {
        ServiceCategory.entries.forEach { category ->
            val dto = serviceDto.toDomain().copy(category = category).toDto()

            assertEquals(category, dto.toDomain().category)
        }
        assertEquals("unas", serviceDto.toDomain().copy(category = ServiceCategory.UNAS).toDto().category)
    }

    @Test
    fun `an unknown stored category degrades to OTRO instead of failing`() {
        assertEquals(ServiceCategory.OTRO, serviceDto.copy(category = "yoga").toDomain().category)
    }
}
