package corp.khin.solutions.booqi.data.repository

import corp.khin.solutions.booqi.core.common.DomainResult
import corp.khin.solutions.booqi.data.datasource.FakeServiceRemoteDataSource
import corp.khin.solutions.booqi.domain.model.Service
import corp.khin.solutions.booqi.domain.model.ServiceCategory
import corp.khin.solutions.booqi.domain.model.ServiceDetails
import corp.khin.solutions.booqi.domain.model.ServiceModality
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

/** Category handling in [ServiceRepositoryImpl]: default on create, "keep" on edit without one. */
class ServiceRepositoryImplCategoryTest {

    private val repository = ServiceRepositoryImpl(FakeServiceRemoteDataSource())

    private fun details(title: String = "Corte", category: ServiceCategory? = null) = ServiceDetails(
        title = title,
        photoUrl = "https://example.com/p.jpg",
        description = "d",
        priceCents = 1000,
        durationMinutes = 30,
        modality = ServiceModality.LOCAL,
        category = category,
    )

    private fun DomainResult<Service>.ok(): Service = (this as DomainResult.Success).value

    @Test
    fun `a service created without a category is OTRO`() = runTest {
        assertEquals(ServiceCategory.OTRO, repository.addService("p1", details()).ok().category)
    }

    @Test
    fun `a service created with a category keeps it through a round trip`() = runTest {
        val created = repository.addService("p1", details(category = ServiceCategory.MASAJES)).ok()

        assertEquals(ServiceCategory.MASAJES, repository.getService(created.id).ok().category)
    }

    @Test
    fun `editing without a category keeps the current one`() = runTest {
        val created = repository.addService("p1", details(category = ServiceCategory.BARBERIA)).ok()

        val edited = repository.updateService(created.id, details(title = "Corte nuevo")).ok()

        assertEquals("Corte nuevo", edited.title)
        assertEquals(ServiceCategory.BARBERIA, edited.category)
    }

    @Test
    fun `editing with a category changes it`() = runTest {
        val created = repository.addService("p1", details(category = ServiceCategory.BARBERIA)).ok()

        assertEquals(
            ServiceCategory.TECNICO,
            repository.updateService(created.id, details(category = ServiceCategory.TECNICO)).ok().category,
        )
    }

    @Test
    fun `disabling and enabling keep the category`() = runTest {
        val created = repository.addService("p1", details(category = ServiceCategory.LIMPIEZA)).ok()

        assertEquals(ServiceCategory.LIMPIEZA, repository.disableService(created.id).ok().category)
        assertEquals(ServiceCategory.LIMPIEZA, repository.enableService(created.id).ok().category)
    }
}
