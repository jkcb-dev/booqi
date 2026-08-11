package corp.khin.solutions.booqi.domain.usecase

import corp.khin.solutions.booqi.core.common.DomainError
import corp.khin.solutions.booqi.core.common.DomainResult
import corp.khin.solutions.booqi.core.common.asFailure
import corp.khin.solutions.booqi.core.common.asSuccess
import corp.khin.solutions.booqi.domain.model.ServiceCategory
import corp.khin.solutions.booqi.domain.model.ServiceProvider
import corp.khin.solutions.booqi.domain.repository.ServiceCatalogRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

/**
 * Pure JVM/common unit test against a fake repository — no coroutines-test ceremony beyond
 * `runTest`, no Compose, no platform dependency. This is the shape every domain test should take.
 */
class GetFeaturedProvidersUseCaseTest {

    private val sampleProvider = ServiceProvider(
        id = "1",
        name = "Jane's Nails",
        category = ServiceCategory.NAILS,
        ratingOutOf5 = 4.8,
        priceFromCents = 3500,
        shortTagline = "Gel & acrylic specialist",
    )

    @Test
    fun `returns providers from repository on success`() = runTest {
        val useCase = GetFeaturedProvidersUseCase(FakeRepository(DomainResult.Success(listOf(sampleProvider))))

        val result = useCase()

        assertIs<DomainResult.Success<List<ServiceProvider>>>(result)
        assertEquals(listOf(sampleProvider), result.value)
    }

    @Test
    fun `propagates repository failure unchanged`() = runTest {
        val useCase = GetFeaturedProvidersUseCase(FakeRepository(DomainError.NoConnection.asFailure()))

        val result = useCase()

        assertIs<DomainResult.Failure>(result)
        assertEquals(DomainError.NoConnection, result.error)
    }

    private class FakeRepository(
        private val result: DomainResult<List<ServiceProvider>>,
    ) : ServiceCatalogRepository {
        override suspend fun getFeaturedProviders(): DomainResult<List<ServiceProvider>> = result
    }
}
