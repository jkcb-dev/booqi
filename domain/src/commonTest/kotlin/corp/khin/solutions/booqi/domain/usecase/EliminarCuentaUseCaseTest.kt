package corp.khin.solutions.booqi.domain.usecase

import corp.khin.solutions.booqi.domain.model.AccountDeletionResult
import corp.khin.solutions.booqi.domain.model.Address
import corp.khin.solutions.booqi.domain.model.BookingStatus
import corp.khin.solutions.booqi.domain.model.Credentials
import corp.khin.solutions.booqi.domain.model.Rating
import corp.khin.solutions.booqi.domain.model.Registration
import corp.khin.solutions.booqi.domain.model.User
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Escenarios: "El Usuario intenta eliminar su cuenta con reservas activas" y "...elimina su cuenta
 * sin reservas activas" (docs/domain/identity-flow.md).
 */
class EliminarCuentaUseCaseTest {

    private val users = FakeUserRepository()
    private val bookings = FakeBookingRepository()
    private val profiles = FakeProviderProfileRepository()
    private val eliminar = EliminarCuentaUseCase(users, bookings, profiles)

    private val me = User("user-1", "Ana", "ana@example.com", photoUrl = "https://example.com/ana.jpg", isEmailVerified = true)

    private suspend fun signedIn() = users.seedSignedIn(me)

    private suspend fun myProfileId() = profiles.activateProviderMode(me.id).value().id

    private suspend fun deleted(): AccountDeletionResult = eliminar().value()

    private suspend fun blocked() = assertIs<AccountDeletionResult.Blocked>(deleted()).activeBookings

    // --- blocked --------------------------------------------------------------------------------

    @Test
    fun `a Requested or Confirmed booking as Customer blocks the deletion and is listed`() = runTest {
        signedIn()
        bookings.seed(booking(id = "b-requested", customerId = me.id, status = BookingStatus.REQUESTED))
        bookings.seed(booking(id = "b-confirmed", customerId = me.id, status = BookingStatus.CONFIRMED))

        val active = blocked()

        assertEquals(listOf("b-confirmed", "b-requested"), active.asCustomer.map { it.id }.sorted())
        assertTrue(active.asProvider.isEmpty())
        assertEquals(me, users.getCurrentUser().value()) // nothing changed, still signed in
        assertEquals(0, users.writeCount)
        assertEquals(0, bookings.writeCount)
    }

    @Test
    fun `a Requested or Confirmed booking as Provider blocks the deletion and is listed`() = runTest {
        signedIn()
        val profileId = myProfileId()
        bookings.seed(booking(id = "b-incoming", providerId = profileId, customerId = "other", status = BookingStatus.REQUESTED))
        bookings.seed(booking(id = "b-agenda", providerId = profileId, customerId = "other", status = BookingStatus.CONFIRMED))

        val active = blocked()

        assertEquals(listOf("b-agenda", "b-incoming"), active.asProvider.map { it.id }.sorted())
        assertTrue(active.asCustomer.isEmpty())
        assertEquals(me, users.getCurrentUser().value())
        assertTrue(profiles.getProfile(profileId).value().let { it.userId == me.id })
    }

    @Test
    fun `active bookings in both roles are all reported by role`() = runTest {
        signedIn()
        val profileId = myProfileId()
        bookings.seed(booking(id = "as-customer", customerId = me.id, providerId = "someone-else"))
        bookings.seed(booking(id = "as-provider", providerId = profileId, customerId = "other"))

        val active = blocked()

        assertEquals(listOf("as-customer"), active.asCustomer.map { it.id })
        assertEquals(listOf("as-provider"), active.asProvider.map { it.id })
    }

    @Test
    fun `finished bookings do not block`() = runTest {
        signedIn()
        val profileId = myProfileId()
        val finished = BookingStatus.entries.filter { !it.isActive }
        finished.forEach { status ->
            bookings.seed(booking(id = "c-$status", customerId = me.id, status = status))
            bookings.seed(booking(id = "p-$status", providerId = profileId, customerId = "other", status = status))
        }

        assertEquals(AccountDeletionResult.Deleted, deleted())
    }

    @Test
    fun `another user's active bookings do not block`() = runTest {
        signedIn()
        bookings.seed(booking(id = "b-1", customerId = "someone-else", providerId = "other-provider"))

        assertEquals(AccountDeletionResult.Deleted, deleted())
    }

    @Test
    fun `once the active bookings are completed the deletion goes through`() = runTest {
        signedIn()
        bookings.seed(booking(id = "b-1", customerId = me.id, status = BookingStatus.CONFIRMED))
        blocked()

        bookings.seed(booking(id = "b-1", customerId = me.id, status = BookingStatus.COMPLETED))

        assertEquals(AccountDeletionResult.Deleted, deleted())
    }

    // --- deleted --------------------------------------------------------------------------------

    @Test
    fun `deleting erases name photo and email flags the account and ends the session`() = runTest {
        signedIn()

        assertEquals(AccountDeletionResult.Deleted, deleted())

        val tombstone = users.stored(me.id)
        assertEquals("", tombstone.displayName)
        assertNull(tombstone.photoUrl)
        assertNull(tombstone.email)
        assertTrue(tombstone.isDeleted)
        assertNull(users.getCurrentUser().value())
        assertEquals(User.DELETED_USER_NAME, tombstone.publicName)
    }

    @Test
    fun `a deleted account can no longer sign in`() = runTest {
        RegistrarUsuarioUseCase(users)(Registration.WithEmail("Ana", "ana@example.com", "secreto123"))
            .value()
        eliminar().value()

        IniciarSesionUseCase(users)(Credentials.WithEmail("ana@example.com", "secreto123")).assertUnauthorized()
    }

    @Test
    fun `completed bookings and their ratings are kept and the providers rating does not change`() = runTest {
        signedIn()
        val providerId = profiles.activateProviderMode("provider-user").value().id
        bookings.seed(
            booking(
                id = "b-1",
                customerId = me.id,
                providerId = providerId,
                status = BookingStatus.COMPLETED,
                completedAt = REQUESTED_AT,
            ),
        )
        CalificarCitaUseCase(bookings, RecalcularCalificacionDelProveedorUseCase(bookings, profiles))("b-1", 5, "Excelente")
            .value()
        val ratingBefore = profiles.getProfile(providerId).value()

        assertEquals(AccountDeletionResult.Deleted, deleted())

        assertEquals(BookingStatus.COMPLETED, bookings.stored("b-1").status)
        assertEquals(Rating(5, "Excelente"), bookings.stored("b-1").rating)
        assertEquals(me.id, bookings.stored("b-1").customerId) // still points at the (anonymized) user
        val ratingAfter = profiles.getProfile(providerId).value()
        assertEquals(ratingBefore.ratingAverage, ratingAfter.ratingAverage)
        assertEquals(ratingBefore.ratingCount, ratingAfter.ratingCount)
        assertEquals(1, bookings.getRatedBookingsByProvider(providerId).value().size)
    }

    @Test
    fun `personal data on the customers past bookings is erased`() = runTest {
        signedIn()
        bookings.seed(
            booking(id = "b-1", customerId = me.id, status = BookingStatus.COMPLETED).copy(
                deliveryAddress = Address("Av. Corrientes 1234", -34.6, -58.4),
                customerNote = "Timbre roto, llamar al celular",
            ),
        )

        deleted()

        assertNull(bookings.stored("b-1").deliveryAddress)
        assertNull(bookings.stored("b-1").customerNote)
        assertEquals(BookingStatus.COMPLETED, bookings.stored("b-1").status)
    }

    @Test
    fun `a Provider profile is anonymized and hidden but keeps its rating summary`() = runTest {
        signedIn()
        val profileId = myProfileId()
        profiles.completeProfile(profileId, "Studio Ana", "https://example.com/p.jpg", "Manicuría", "Av. Santa Fe 1")
        profiles.updateRating(profileId, 4.5, 2)

        deleted()

        val profile = profiles.getProfile(profileId).value()
        assertEquals(false, profile.isComplete) // the Catalog's visibility gate
        assertNull(profile.name)
        assertNull(profile.photoUrl)
        assertNull(profile.description)
        assertNull(profile.location)
        assertEquals(4.5, profile.ratingAverage)
        assertEquals(2, profile.ratingCount)
    }

    @Test
    fun `deleting without a session is Unauthorized`() = runTest {
        eliminar().assertUnauthorized()
    }
}
