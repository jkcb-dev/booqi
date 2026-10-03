package corp.khin.solutions.booqi.data.repository

import corp.khin.solutions.booqi.core.common.DomainResult
import corp.khin.solutions.booqi.data.datasource.FakeProviderProfileRemoteDataSource
import corp.khin.solutions.booqi.data.datasource.FakeServiceRemoteDataSource
import corp.khin.solutions.booqi.data.datasource.SampleData
import corp.khin.solutions.booqi.data.dto.ProviderProfileDto
import corp.khin.solutions.booqi.data.dto.ServiceDto
import corp.khin.solutions.booqi.domain.model.CatalogEntry
import corp.khin.solutions.booqi.domain.model.GeoPoint
import corp.khin.solutions.booqi.domain.model.ServiceCategory
import corp.khin.solutions.booqi.domain.usecase.BuscarServiciosUseCase
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** The Catalog join: `Service.providerId == ProviderProfile.id`, no fallback by user id. */
class CatalogRepositoryImplTest {

    private fun profile(id: String, userId: String, complete: Boolean = true) = ProviderProfileDto(
        id = id,
        userId = userId,
        name = "Proveedor $id",
        photoUrl = null,
        description = null,
        location = "Calle 1",
        isComplete = complete,
        pausedRangeStart = null,
        pausedRangeEnd = null,
        ratingAverage = 4.0,
        ratingCount = 3,
        locationLat = -34.6,
        locationLng = -58.4,
    )

    private fun service(id: String, providerId: String, active: Boolean = true) = ServiceDto(
        id = id,
        providerId = providerId,
        title = "Servicio $id",
        photoUrl = "https://example.com/$id.jpg",
        description = "d",
        priceCents = 1000,
        durationMinutes = 30,
        modality = "local",
        isActive = active,
        category = "unas",
    )

    private fun entries(result: DomainResult<List<CatalogEntry>>) = (result as DomainResult.Success).value

    @Test
    fun `it joins each service to the profile whose id is its providerId`() = runTest {
        val repository = CatalogRepositoryImpl(
            FakeServiceRemoteDataSource(
                seed = listOf(service("s1", "p1"), service("s2", "p2"), service("s3", "p1")),
            ),
            FakeProviderProfileRemoteDataSource(seed = listOf(profile("p1", "u1"), profile("p2", "u2"))),
        )

        val result = entries(repository.getEntries())

        assertEquals(listOf("s1", "s2", "s3"), result.map { it.service.id })
        assertEquals(listOf("p1", "p2", "p1"), result.map { it.provider.id })
        assertEquals(ServiceCategory.UNAS, result.first().service.category)
        assertEquals(GeoPoint(-34.6, -58.4), result.first().provider.coordinates)
    }

    @Test
    fun `a service whose providerId is a user id and not a profile id is dropped`() = runTest {
        val repository = CatalogRepositoryImpl(
            FakeServiceRemoteDataSource(seed = listOf(service("s1", providerId = "u1"))),
            FakeProviderProfileRemoteDataSource(seed = listOf(profile("p1", userId = "u1"))),
        )

        assertTrue(entries(repository.getEntries()).isEmpty())
    }

    @Test
    fun `it does not apply visibility rules itself`() = runTest {
        val repository = CatalogRepositoryImpl(
            FakeServiceRemoteDataSource(seed = listOf(service("off", "p1", active = false), service("draft", "p2"))),
            FakeProviderProfileRemoteDataSource(
                seed = listOf(profile("p1", "u1"), profile("p2", "u2", complete = false)),
            ),
        )

        assertEquals(listOf("off", "draft"), entries(repository.getEntries()).map { it.service.id })
    }

    @Test
    fun `the sample data joins and gives the catalog demo results with each category and coordinates`() = runTest {
        val repository = CatalogRepositoryImpl(
            FakeServiceRemoteDataSource(seed = SampleData.services()),
            FakeProviderProfileRemoteDataSource(seed = SampleData.providerProfiles()),
        )

        val results = BuscarServiciosUseCase(repository, timeZone = TimeZone.UTC)().let {
            (it as DomainResult.Success).value
        }

        // The provider-side placeholder services (providerId = user id) must not join until #50.
        assertTrue(results.none { it.service.providerId == SampleData.PROVIDER_ID })
        assertEquals(ServiceCategory.filterable.toSet(), results.map { it.service.category }.toSet())
        assertTrue(results.all { it.provider.name.isNotBlank() })
    }
}
