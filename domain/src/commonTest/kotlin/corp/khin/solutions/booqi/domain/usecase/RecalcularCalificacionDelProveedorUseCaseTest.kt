package corp.khin.solutions.booqi.domain.usecase

import corp.khin.solutions.booqi.domain.model.BookingStatus
import corp.khin.solutions.booqi.domain.model.Rating
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Second half of "El Cliente califica una cita completada" (docs/domain/provider-flow.md § Grupo 4):
 * the Provider's average and count are recomputed from all their rated bookings (a computed
 * aggregate per docs/DOMAIN.md), never incrementally.
 */
class RecalcularCalificacionDelProveedorUseCaseTest {

    private val bookings = FakeBookingRepository()
    private val profiles = FakeProviderProfileRepository()
    private val recalcular = RecalcularCalificacionDelProveedorUseCase(bookings, profiles)
    private val calificar = CalificarCitaUseCase(bookings, recalcular)

    private suspend fun provider(user: String = "user-1") = profiles.activateProviderMode(user).value().id

    private fun rated(id: String, providerId: String, stars: Int) = bookings.seed(
        booking(id = id, providerId = providerId, status = BookingStatus.COMPLETED, rating = Rating(stars)),
    )

    @Test
    fun `the average is the mean of all rated bookings and the count is how many`() = runTest {
        val providerId = provider()
        rated("b1", providerId, 5)
        rated("b2", providerId, 4)
        rated("b3", providerId, 3)

        val profile = recalcular(providerId).value()

        assertEquals(4.0, profile.ratingAverage)
        assertEquals(3, profile.ratingCount)
    }

    @Test
    fun `the average is not rounded`() = runTest {
        val providerId = provider()
        rated("b1", providerId, 5)
        rated("b2", providerId, 4)
        rated("b3", providerId, 4)

        assertEquals(13.0 / 3, recalcular(providerId).value().ratingAverage)
    }

    @Test
    fun `it is recomputed from the bookings and overwrites a drifted stored value`() = runTest {
        val providerId = provider()
        profiles.updateRating(providerId, 1.0, 99) // wrong on purpose
        rated("b1", providerId, 2)
        rated("b2", providerId, 4)

        val profile = recalcular(providerId).value()

        assertEquals(3.0, profile.ratingAverage)
        assertEquals(2, profile.ratingCount)
    }

    @Test
    fun `successive ratings keep the average equal to the mean of everything rated so far`() = runTest {
        val providerId = provider()
        for ((index, stars) in listOf(5, 4, 3, 2).withIndex()) {
            bookings.seed(booking(id = "b$index", providerId = providerId, status = BookingStatus.COMPLETED))
            calificar("b$index", stars).value()
        }

        val profile = profiles.getProfile(providerId).value()

        assertEquals(3.5, profile.ratingAverage)
        assertEquals(4, profile.ratingCount)
    }

    @Test
    fun `unrated and other providers bookings are not counted`() = runTest {
        val providerId = provider()
        val other = provider("user-2")
        rated("b1", providerId, 5)
        rated("b2", other, 1)
        bookings.seed(booking(id = "b3", providerId = providerId, status = BookingStatus.COMPLETED))
        bookings.seed(booking(id = "b4", providerId = providerId, status = BookingStatus.CONFIRMED))

        val profile = recalcular(providerId).value()

        assertEquals(5.0, profile.ratingAverage)
        assertEquals(1, profile.ratingCount)
        assertNull(profiles.getProfile(other).value().ratingAverage) // untouched
    }

    @Test
    fun `a provider with no ratings ends with no average and a zero count`() = runTest {
        val providerId = provider()
        profiles.updateRating(providerId, 4.0, 2)

        val profile = recalcular(providerId).value()

        assertNull(profile.ratingAverage)
        assertEquals(0, profile.ratingCount)
    }

    @Test
    fun `an unknown provider is NotFound`() = runTest {
        recalcular("ghost").assertNotFound()
    }
}
