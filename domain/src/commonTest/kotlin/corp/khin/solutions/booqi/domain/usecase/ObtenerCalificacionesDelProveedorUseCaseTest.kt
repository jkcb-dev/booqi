package corp.khin.solutions.booqi.domain.usecase

import corp.khin.solutions.booqi.domain.model.BookingStatus
import corp.khin.solutions.booqi.domain.model.Rating
import kotlinx.coroutines.test.runTest
import kotlin.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals

/** Query behind the reviews list (Figma P11): rated bookings of the provider, newest completion first. */
class ObtenerCalificacionesDelProveedorUseCaseTest {

    private val repository = FakeBookingRepository()
    private val obtener = ObtenerCalificacionesDelProveedorUseCase(repository)

    private fun rated(id: String, completedAt: String, stars: Int = 5, providerId: String = "provider-1") =
        repository.seed(
            booking(
                id = id,
                providerId = providerId,
                status = BookingStatus.COMPLETED,
                rating = Rating(stars, "Comentario $id"),
                completedAt = Instant.parse(completedAt),
            ),
        )

    @Test
    fun `it returns only rated bookings of the provider newest completion first`() = runTest {
        rated("old", "2026-10-12T11:00:00Z")
        rated("new", "2026-10-26T11:00:00Z")
        rated("mid", "2026-10-19T11:00:00Z")
        rated("elsewhere", "2026-10-30T11:00:00Z", providerId = "provider-2")
        repository.seed(booking(id = "unrated", status = BookingStatus.COMPLETED))
        repository.seed(booking(id = "confirmed", status = BookingStatus.CONFIRMED))

        assertEquals(listOf("new", "mid", "old"), obtener("provider-1").value().map { it.id })
    }

    @Test
    fun `each review carries its stars and comment`() = runTest {
        rated("r1", "2026-10-12T11:00:00Z", stars = 3)

        assertEquals(Rating(3, "Comentario r1"), obtener("provider-1").value().single().rating)
    }

    @Test
    fun `a provider with no ratings gets an empty list`() = runTest {
        assertEquals(emptyList(), obtener("provider-1").value())
    }
}
