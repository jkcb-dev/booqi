package corp.khin.solutions.booqi.domain.usecase

import corp.khin.solutions.booqi.core.common.DomainError
import corp.khin.solutions.booqi.core.common.DomainResult
import corp.khin.solutions.booqi.domain.model.AccountDeletionResult
import corp.khin.solutions.booqi.domain.model.Address
import corp.khin.solutions.booqi.domain.model.BookingStatus
import corp.khin.solutions.booqi.domain.model.User
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

/**
 * Escenarios de "Grupo 2: Dirección del Cliente" (docs/domain/customer-flow.md): add the single
 * saved address, read it back (what a Domicilio request uses without asking again), replace it
 * without touching bookings already requested — plus the edges: no session, validation, one address
 * only, and the account deletion erasing it. The map picker (#23/#24) and the request wiring (#26)
 * are not part of this ticket.
 */
class DireccionDelClienteUseCasesTest {

    private val users = FakeUserRepository()
    private val bookings = FakeBookingRepository()
    private val agregar = AgregarDireccionUseCase(users)
    private val actualizar = ActualizarDireccionUseCase(users)
    private val obtener = ObtenerMiDireccionUseCase(users)

    private val me = User("user-1", "Ana", "ana@example.com", isEmailVerified = true)
    private val casa = Address("Av. Corrientes 1234, Buenos Aires", -34.6037, -58.3816)
    private val oficina = Address("Av. Santa Fe 900, Buenos Aires", -34.5956, -58.3777)

    private fun signedIn() = users.seedSignedIn(me)

    // --- Escenario: El Cliente agrega su dirección al solicitar una reserva a domicilio ----------

    @Test
    fun `a Customer without an address adds one and it is saved in their profile`() = runTest {
        signedIn()
        assertNull(obtener().value()) // "no tiene una dirección guardada"

        val saved = agregar(casa).value()

        assertEquals(casa, saved.address)
        assertEquals(casa, users.stored(me.id).address)
        assertEquals(casa, obtener().value()) // "se usa para esta reserva"
    }

    @Test
    fun `the line is trimmed and the pin is kept`() = runTest {
        signedIn()

        val saved = agregar(casa.copy(line = "  ${casa.line}  ")).value()

        assertEquals(casa, saved.address)
    }

    // --- Escenario: El Cliente ya tiene dirección guardada al solicitar a domicilio -------------

    @Test
    fun `a Customer with a saved address gets it back without being asked again`() = runTest {
        users.seedSignedIn(me.copy(address = casa))

        assertEquals(casa, obtener().value())
    }

    // --- Escenario: El Cliente actualiza su dirección guardada ----------------------------------

    @Test
    fun `updating replaces the previous address and keeps only one`() = runTest {
        users.seedSignedIn(me.copy(address = casa))

        val saved = actualizar(oficina).value()

        assertEquals(oficina, saved.address)
        assertEquals(oficina, obtener().value())
        assertEquals(oficina, users.stored(me.id).address)
    }

    @Test
    fun `bookings already requested keep the address they had`() = runTest {
        users.seedSignedIn(me.copy(address = casa))
        bookings.seed(booking(id = "b-1", customerId = me.id, status = BookingStatus.REQUESTED).copy(deliveryAddress = casa))

        actualizar(oficina).value()

        assertEquals(casa, bookings.stored("b-1").deliveryAddress)
        assertEquals(oficina, obtener().value())
    }

    // --- Edges ----------------------------------------------------------------------------------

    @Test
    fun `without a session every address operation is Unauthorized`() = runTest {
        agregar(casa).assertUnauthorized()
        actualizar(casa).assertUnauthorized()
        obtener().assertUnauthorized()
        assertEquals(0, users.writeCount)
    }

    @Test
    fun `a deleted account cannot use the address operations`() = runTest {
        users.seedSignedIn(me.anonymized())

        agregar(casa).assertUnauthorized()
        actualizar(casa).assertUnauthorized()
        obtener().assertUnauthorized()
    }

    @Test
    fun `a blank line is InvalidInput and nothing is written`() = runTest {
        signedIn()

        assertEquals("La dirección es obligatoria", agregar(casa.copy(line = "   ")).invalidInput())
        assertEquals("La dirección es obligatoria", actualizar(casa.copy(line = "")).invalidInput())

        assertEquals(0, users.writeCount)
        assertNull(users.stored(me.id).address)
    }

    @Test
    fun `coordinates out of range or not a number are InvalidInput`() = runTest {
        signedIn()
        val invalid = listOf(
            casa.copy(latitude = 91.0),
            casa.copy(longitude = -181.0),
            casa.copy(latitude = Double.NaN),
            casa.copy(longitude = Double.POSITIVE_INFINITY),
        )

        invalid.forEach { assertEquals("La ubicación de la dirección no es válida", agregar(it).invalidInput()) }

        assertEquals(0, users.writeCount)
    }

    @Test
    fun `validation comes before the session check`() = runTest {
        assertEquals("La dirección es obligatoria", agregar(casa.copy(line = " ")).invalidInput())
    }

    @Test
    fun `adding when an address is already saved is InvalidInput and keeps the saved one`() = runTest {
        users.seedSignedIn(me.copy(address = casa))

        assertEquals(AgregarDireccionUseCase.ALREADY_HAS_ADDRESS_MESSAGE, agregar(oficina).invalidInput())

        assertEquals(casa, users.stored(me.id).address)
        assertEquals(0, users.writeCount)
    }

    @Test
    fun `updating when there is no address yet is NotFound`() = runTest {
        signedIn()

        val result = actualizar(casa)

        assertIs<DomainResult.Failure>(result)
        assertEquals(DomainError.NotFound, result.error)
        assertNull(users.stored(me.id).address)
    }

    @Test
    fun `an address operation touches only the signed in user`() = runTest {
        users.seedSignedIn(User("user-2", "Beto", "beto@example.com", address = oficina))
        users.seedSignedIn(me)

        agregar(casa).value()

        assertEquals(oficina, users.stored("user-2").address)
    }

    // --- Account deletion (#56) erases it -------------------------------------------------------

    @Test
    fun `deleting the account erases the saved address from the tombstone`() = runTest {
        users.seedSignedIn(me.copy(address = casa))
        val eliminar = EliminarCuentaUseCase(users, bookings, FakeProviderProfileRepository())

        assertEquals(AccountDeletionResult.Deleted, eliminar().value())

        assertNull(users.stored(me.id).address)
        obtener().assertUnauthorized() // the session ended too
    }

    @Test
    fun `a blocked deletion keeps the saved address`() = runTest {
        users.seedSignedIn(me.copy(address = casa))
        bookings.seed(booking(id = "b-1", customerId = me.id, status = BookingStatus.CONFIRMED))
        val eliminar = EliminarCuentaUseCase(users, bookings, FakeProviderProfileRepository())

        assertIs<AccountDeletionResult.Blocked>(eliminar().value())

        assertEquals(casa, users.stored(me.id).address)
    }
}
