package corp.khin.solutions.booqi.domain.usecase

import corp.khin.solutions.booqi.domain.model.BookingStatus
import corp.khin.solutions.booqi.domain.model.Rating
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Escenarios: "El Cliente califica una cita completada" and "El Cliente intenta calificar una cita
 * no completada" (docs/domain/provider-flow.md § Grupo 4), plus once-only and the 1..5 bounds. The
 * average recomputation is covered in depth by `RecalcularCalificacionDelProveedorUseCaseTest`;
 * here we assert it happens as part of rating.
 */
class CalificarCitaUseCaseTest {

    private val bookings = FakeBookingRepository()
    private val profiles = FakeProviderProfileRepository()
    private val calificar = CalificarCitaUseCase(bookings, RecalcularCalificacionDelProveedorUseCase(bookings, profiles))

    private suspend fun provider() = profiles.activateProviderMode("user-1").value().id

    private fun completed(id: String, providerId: String) =
        bookings.seed(booking(id = id, providerId = providerId, status = BookingStatus.COMPLETED))

    @Test
    fun `rating a completed booking attaches the rating and updates the provider average`() = runTest {
        val providerId = provider()
        completed("booking-1", providerId)

        val rated = calificar("booking-1", 4, "Muy buen servicio").value()

        assertEquals(Rating(4, "Muy buen servicio"), rated.rating)
        assertEquals(rated, bookings.stored("booking-1"))
        val profile = profiles.getProfile(providerId).value()
        assertEquals(4.0, profile.ratingAverage)
        assertEquals(1, profile.ratingCount)
    }

    @Test
    fun `the comment is optional`() = runTest {
        completed("booking-1", provider())

        assertNull(calificar("booking-1", 5).value().rating?.comment)
    }

    @Test
    fun `rating a requested or confirmed booking is rejected with an explanation and changes nothing`() = runTest {
        val providerId = provider()
        for (status in listOf(BookingStatus.REQUESTED, BookingStatus.CONFIRMED)) {
            bookings.seed(booking(id = "b-$status", providerId = providerId, status = status))

            val message = calificar("b-$status", 5).invalidInput()

            assertTrue(message.contains("completada"), message)
            assertNull(bookings.stored("b-$status").rating)
        }
        assertEquals(0, bookings.writeCount)
        assertEquals(0, profiles.getProfile(providerId).value().ratingCount)
    }

    @Test
    fun `rating a booking in any other state than completed is rejected`() = runTest {
        val providerId = provider()
        for (status in BookingStatus.entries.filter { it != BookingStatus.COMPLETED }) {
            bookings.seed(booking(id = "b-$status", providerId = providerId, status = status))

            calificar("b-$status", 3).invalidInput()
        }
        assertEquals(0, bookings.writeCount)
    }

    @Test
    fun `a booking can be rated only once and the first rating stays`() = runTest {
        val providerId = provider()
        completed("booking-1", providerId)
        calificar("booking-1", 2, "Regular").value()
        val writes = bookings.writeCount

        calificar("booking-1", 5, "Cambié de opinión").invalidInput()

        assertEquals(Rating(2, "Regular"), bookings.stored("booking-1").rating)
        assertEquals(writes, bookings.writeCount)
        assertEquals(1, profiles.getProfile(providerId).value().ratingCount)
    }

    @Test
    fun `stars outside 1 to 5 are rejected before anything is saved`() = runTest {
        val providerId = provider()
        completed("booking-1", providerId)

        for (stars in listOf(0, 6, -3)) {
            calificar("booking-1", stars).invalidInput()
        }

        assertEquals(0, bookings.writeCount)
        assertNull(bookings.stored("booking-1").rating)
    }

    @Test
    fun `the lowest and highest stars are accepted`() = runTest {
        val providerId = provider()
        completed("booking-1", providerId)
        completed("booking-2", providerId)

        assertEquals(1, calificar("booking-1", 1).value().rating?.stars)
        assertEquals(5, calificar("booking-2", 5).value().rating?.stars)
        assertEquals(3.0, profiles.getProfile(providerId).value().ratingAverage)
    }

    @Test
    fun `rating an unknown booking is NotFound`() = runTest {
        calificar("missing", 5).assertNotFound()
    }

    @Test
    fun `if the profile cannot be updated the failure is returned but the rating is already saved`() = runTest {
        completed("booking-1", "ghost-provider") // no ProviderProfile with this id

        calificar("booking-1", 5).assertNotFound()

        assertEquals(Rating(5), bookings.stored("booking-1").rating)
    }
}
