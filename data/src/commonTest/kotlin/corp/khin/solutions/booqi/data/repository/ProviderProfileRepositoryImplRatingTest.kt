package corp.khin.solutions.booqi.data.repository

import corp.khin.solutions.booqi.core.common.DomainError
import corp.khin.solutions.booqi.core.common.DomainResult
import corp.khin.solutions.booqi.data.datasource.FakeProviderProfileRemoteDataSource
import corp.khin.solutions.booqi.domain.model.ProviderProfile
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** The read and rating methods added to [ProviderProfileRepositoryImpl] for the Booking flow (#18). */
class ProviderProfileRepositoryImplRatingTest {

    private val repository = ProviderProfileRepositoryImpl(FakeProviderProfileRemoteDataSource())

    private fun DomainResult<ProviderProfile>.ok(): ProviderProfile = (this as DomainResult.Success).value

    @Test
    fun `getProfile returns the stored profile or NotFound`() = runTest {
        val created = repository.activateProviderMode("user-1").ok()

        assertEquals(created, repository.getProfile(created.id).ok())
        assertEquals(DomainError.NotFound, (repository.getProfile("missing") as DomainResult.Failure).error)
    }

    @Test
    fun `updateRating stores the summary and leaves the rest of the profile alone`() = runTest {
        val created = repository.activateProviderMode("user-1").ok()
        repository.setPausedRange(created.id, null)

        val updated = repository.updateRating(created.id, 4.5, 2).ok()

        assertEquals(4.5, updated.ratingAverage)
        assertEquals(2, updated.ratingCount)
        assertEquals(created.copy(ratingAverage = 4.5, ratingCount = 2), repository.getProfile(created.id).ok())
    }

    @Test
    fun `updateRating can clear the average and a missing profile is NotFound`() = runTest {
        val created = repository.activateProviderMode("user-1").ok()
        repository.updateRating(created.id, 5.0, 1)

        assertNull(repository.updateRating(created.id, null, 0).ok().ratingAverage)
        assertEquals(
            DomainError.NotFound,
            (repository.updateRating("missing", 3.0, 1) as DomainResult.Failure).error,
        )
    }
}
