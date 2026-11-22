package corp.khin.solutions.booqi.data.repository

import corp.khin.solutions.booqi.core.common.DomainResult
import corp.khin.solutions.booqi.data.datasource.FakeProviderProfileRemoteDataSource
import corp.khin.solutions.booqi.domain.model.GeoPoint
import corp.khin.solutions.booqi.domain.model.ProviderLocation
import corp.khin.solutions.booqi.domain.model.ProviderProfile
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** `completeProfile` persists the location pin through the DTO (`location_lat/lng`) and round-trips it. */
class ProviderProfileRepositoryImplLocationTest {

    private val repository = ProviderProfileRepositoryImpl(FakeProviderProfileRemoteDataSource())

    private fun DomainResult<ProviderProfile>.ok(): ProviderProfile = (this as DomainResult.Success).value

    @Test
    fun `completeProfile stores the pin and getProfile reads it back`() = runTest {
        val id = repository.activateProviderMode("user-1").ok().id
        val pin = GeoPoint(-34.6037, -58.3816)

        repository.completeProfile(id, "Studio", "https://example.com/p.jpg", "desc", ProviderLocation("Centro", pin))

        val stored = repository.getProfile(id).ok()
        assertEquals("Centro", stored.location)
        assertEquals(pin, stored.coordinates)
    }

    @Test
    fun `completeProfile with null coordinates clears a previous pin`() = runTest {
        val id = repository.activateProviderMode("user-1").ok().id
        repository.completeProfile(id, "S", "u", "d", ProviderLocation("Centro", GeoPoint(-34.6, -58.4)))

        repository.completeProfile(id, "S", "u", "d", ProviderLocation("Otra calle"))

        assertNull(repository.getProfile(id).ok().coordinates)
    }
}
