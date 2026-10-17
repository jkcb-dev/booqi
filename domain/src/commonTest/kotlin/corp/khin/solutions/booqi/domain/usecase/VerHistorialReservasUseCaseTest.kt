package corp.khin.solutions.booqi.domain.usecase

import corp.khin.solutions.booqi.domain.model.BookingHistory
import corp.khin.solutions.booqi.domain.model.BookingStatus
import corp.khin.solutions.booqi.domain.model.User
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Escenario: "El Cliente consulta su historial de reservas" (docs/domain/customer-flow.md § Grupo
 * 3) — Bookings separated by status — plus the deterministic order, scoping to the current
 * Customer and the missing-session case.
 */
class VerHistorialReservasUseCaseTest {

    private val users = FakeUserRepository()
    private val repository = FakeBookingRepository()
    private val historial = VerHistorialReservasUseCase(ObtenerUsuarioActualUseCase(users), repository)

    private val me = User("customer-1", "Ana", "ana@example.com", isEmailVerified = true)

    private fun at(day: Int, hour: Int = 10) = LocalDateTime(2026, 10, day, hour, 0)

    private fun seed(id: String, status: BookingStatus, scheduledAt: LocalDateTime, customerId: String = me.id) =
        repository.seed(booking(id = id, status = status, scheduledAt = scheduledAt, customerId = customerId))

    @Test
    fun `bookings in different statuses are separated by status`() = runTest {
        users.seedSignedIn(me)
        seed("pending", BookingStatus.REQUESTED, at(20))
        seed("confirmed", BookingStatus.CONFIRMED, at(21))
        seed("completed", BookingStatus.COMPLETED, at(10))
        seed("cancelled", BookingStatus.CANCELLED_BY_CUSTOMER, at(11))

        val history = historial().value()

        assertEquals(
            listOf(
                BookingStatus.REQUESTED,
                BookingStatus.CONFIRMED,
                BookingStatus.COMPLETED,
                BookingStatus.CANCELLED_BY_CUSTOMER,
            ),
            history.groups.map { it.status },
        )
        assertEquals(listOf("pending"), history.group(BookingStatus.REQUESTED)?.bookings?.map { it.id })
        assertEquals(listOf("cancelled"), history.group(BookingStatus.CANCELLED_BY_CUSTOMER)?.bookings?.map { it.id })
        assertNull(history.group(BookingStatus.EXPIRED))
    }

    @Test
    fun `groups follow the fixed status order whatever the insertion order`() = runTest {
        users.seedSignedIn(me)
        BookingStatus.entries.reversed().forEachIndexed { index, status -> seed("b-$index", status, at(10 + index)) }

        assertEquals(BookingHistory.STATUS_ORDER, historial().value().groups.map { it.status })
        assertEquals(BookingStatus.entries.toSet(), BookingHistory.STATUS_ORDER.toSet())
    }

    @Test
    fun `open bookings list the soonest first and closed ones the most recent first with ties by id`() = runTest {
        users.seedSignedIn(me)
        seed("c", BookingStatus.CONFIRMED, at(25))
        seed("a", BookingStatus.CONFIRMED, at(22))
        seed("b", BookingStatus.CONFIRMED, at(22))
        seed("old", BookingStatus.COMPLETED, at(1))
        seed("recent", BookingStatus.COMPLETED, at(8))
        seed("z", BookingStatus.COMPLETED, at(5, 9))
        seed("y", BookingStatus.COMPLETED, at(5, 9))

        val history = historial().value()

        assertEquals(listOf("a", "b", "c"), history.group(BookingStatus.CONFIRMED)?.bookings?.map { it.id })
        assertEquals(listOf("recent", "y", "z", "old"), history.group(BookingStatus.COMPLETED)?.bookings?.map { it.id })
        assertEquals(
            listOf("a", "b", "c", "recent", "y", "z", "old"),
            history.bookings.map { it.id },
        )
    }

    @Test
    fun `only the current Customer's bookings are returned`() = runTest {
        users.seedSignedIn(me)
        seed("mine", BookingStatus.CONFIRMED, at(20))
        seed("theirs", BookingStatus.CONFIRMED, at(21), customerId = "customer-2")

        assertEquals(listOf("mine"), historial().value().bookings.map { it.id })
    }

    @Test
    fun `a Customer without bookings gets an empty history not an error`() = runTest {
        users.seedSignedIn(me)

        val history = historial().value()

        assertTrue(history.isEmpty)
        assertTrue(history.bookings.isEmpty())
    }

    @Test
    fun `without a session the history is Unauthorized`() = runTest {
        seed("mine", BookingStatus.CONFIRMED, at(20))

        historial().assertUnauthorized()
    }
}
