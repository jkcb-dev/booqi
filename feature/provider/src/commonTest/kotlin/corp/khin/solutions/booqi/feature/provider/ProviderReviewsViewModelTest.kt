@file:OptIn(ExperimentalCoroutinesApi::class)

package corp.khin.solutions.booqi.feature.provider

import corp.khin.solutions.booqi.core.common.DomainError
import corp.khin.solutions.booqi.domain.model.Booking
import corp.khin.solutions.booqi.domain.model.BookingStatus
import corp.khin.solutions.booqi.domain.model.Rating
import corp.khin.solutions.booqi.domain.usecase.ObtenerCalificacionesDelProveedorUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Grupo 4 scenario "El Proveedor consulta sus calificaciones" (docs/domain/provider-flow.md) as it
 * surfaces on the profile's P11 section, through [ProviderReviewsViewModel]'s reducer.
 */
class ProviderReviewsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val repository = FakeBookingRepository()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun started(): ProviderReviewsViewModel =
        ProviderReviewsViewModel(ObtenerCalificacionesDelProveedorUseCase(repository)).also {
            it.onAction(ProviderReviewsAction.Start)
            testDispatcher.scheduler.advanceUntilIdle()
        }

    private fun rated(
        id: String,
        stars: Int,
        comment: String? = null,
        completedAt: String = "2026-09-30T10:00:00Z",
        providerId: String = TEMPORARY_PROVIDER_ID,
    ) = Booking(
        id = id,
        providerId = providerId,
        serviceId = "service-1",
        customerId = "customer-1",
        scheduledAt = LocalDateTime(2026, 9, 30, 9, 0),
        durationMinutesSnapshot = 60,
        priceCentsSnapshot = 1250,
        requestedAt = Instant.parse("2026-09-28T10:00:00Z"),
        status = BookingStatus.COMPLETED,
        rating = Rating(stars, comment),
        completedAt = Instant.parse(completedAt),
    )

    @Test
    fun `shows the average the count and the reviews newest first`() = runTest(testDispatcher) {
        repository.seed(rated("old", 5, "Excelente", completedAt = "2026-09-01T10:00:00Z"))
        repository.seed(rated("mid", 4, completedAt = "2026-09-15T10:00:00Z"))
        repository.seed(rated("new", 3, "Normal", completedAt = "2026-09-30T10:00:00Z"))
        repository.seed(rated("ajena", 1, providerId = "otro"))

        val viewModel = started()

        val state = viewModel.state.value
        assertFalse(state.isLoading)
        assertNull(state.error)
        assertEquals(listOf("new", "mid", "old"), state.reviews.map { it.id })
        assertEquals(3, state.count)
        assertEquals(4.0, state.average)
        assertEquals(listOf("Normal", null, "Excelente"), state.reviews.map { it.rating?.comment })
    }

    @Test
    fun `a provider without ratings gets an empty list no average and no error`() = runTest(testDispatcher) {
        val viewModel = started()

        val state = viewModel.state.value
        assertTrue(state.reviews.isEmpty())
        assertEquals(0, state.count)
        assertNull(state.average)
        assertNull(state.error)
        assertFalse(state.isLoading)
    }

    @Test
    fun `entering again picks up a rating left in the meantime`() = runTest(testDispatcher) {
        repository.seed(rated("one", 5))
        val viewModel = started()
        assertEquals(1, viewModel.state.value.count)

        repository.seed(rated("two", 2, "Mejorable"))
        viewModel.onAction(ProviderReviewsAction.Start)
        advanceUntilIdle()

        assertEquals(2, viewModel.state.value.count)
        assertEquals(3.5, viewModel.state.value.average)
    }

    @Test
    fun `a failing load exposes the error and a retry recovers`() = runTest(testDispatcher) {
        repository.seed(rated("one", 4))
        repository.listFailure = DomainError.NoConnection
        val viewModel = started()

        assertEquals(DomainError.NoConnection, viewModel.state.value.error)
        assertTrue(viewModel.state.value.reviews.isEmpty())

        repository.listFailure = null
        viewModel.onAction(ProviderReviewsAction.Start)
        advanceUntilIdle()

        assertNull(viewModel.state.value.error)
        assertEquals(4.0, viewModel.state.value.average)
    }

    @Test
    fun `a review maps to the design system model with a neutral author and the completion date`() {
        val review = assertNotNull(rated("one", 5, "Genial").toRatingReview(TimeZone.UTC))

        assertEquals(5, review.stars)
        assertEquals("Genial", review.comment)
        assertEquals("Cliente", review.author)
        assertEquals("30 de septiembre de 2026", review.dateLabel)
    }
}
