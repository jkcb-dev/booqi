@file:OptIn(ExperimentalCoroutinesApi::class)

package corp.khin.solutions.booqi.feature.account

import corp.khin.solutions.booqi.core.common.DomainResult
import corp.khin.solutions.booqi.domain.model.Booking
import corp.khin.solutions.booqi.domain.model.BookingStatus
import corp.khin.solutions.booqi.domain.model.ProviderProfile
import corp.khin.solutions.booqi.domain.model.Service
import corp.khin.solutions.booqi.domain.model.ServiceModality
import corp.khin.solutions.booqi.domain.model.User
import corp.khin.solutions.booqi.domain.usecase.EliminarCuentaUseCase
import corp.khin.solutions.booqi.domain.usecase.ObtenerServicioUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.LocalDateTime
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant

/**
 * The account-deletion scenarios of docs/domain/identity-flow.md ("...con reservas activas", "...sin
 * reservas activas") through [DeleteAccountViewModel]'s reducer over the real [EliminarCuentaUseCase].
 */
class DeleteAccountViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val users = FakeUserRepository()
    private val bookings = FakeBookingRepository()
    private val profiles = FakeProviderProfileRepository()
    private val services = FakeServiceRepository(
        listOf(
            Service(
                id = "service-1",
                providerId = "profile-1",
                title = "Manicure gel",
                photoUrl = "https://example.com/gel.jpg",
                description = "Manicure",
                priceCents = 2500,
                durationMinutes = 45,
                modality = ServiceModality.LOCAL,
            ),
        ),
    )

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        users.seed(User(id = "u1", displayName = "Ana Pérez", email = "ana@booqi.test", isEmailVerified = true), signedIn = true)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun newViewModel() = DeleteAccountViewModel(
        eliminarCuenta = EliminarCuentaUseCase(users, bookings, profiles),
        obtenerServicio = ObtenerServicioUseCase(services),
    ).also { it.onAction(DeleteAccountAction.Start) }

    private fun booking(
        id: String,
        status: BookingStatus,
        customerId: String = "u1",
        providerId: String = "profile-1",
    ) = Booking(
        id = id,
        providerId = providerId,
        serviceId = "service-1",
        customerId = customerId,
        scheduledAt = LocalDateTime(2026, 11, 5, 10, 0),
        durationMinutesSnapshot = 45,
        priceCentsSnapshot = 2500,
        requestedAt = Instant.parse("2026-10-01T10:00:00Z"),
        status = status,
    )

    @Test
    fun `the flow starts closed`() {
        val viewModel = newViewModel()

        assertFalse(viewModel.state.value.isOpen)
    }

    @Test
    fun `asking to delete opens the confirmation and deletes nothing yet`() = runTest(testDispatcher) {
        val viewModel = newViewModel()

        viewModel.onAction(DeleteAccountAction.DeleteClicked)

        assertTrue(viewModel.state.value.isConfirming)
        assertNotNull(users.current)
    }

    @Test
    fun `dismissing the confirmation closes it and keeps the account`() = runTest(testDispatcher) {
        val viewModel = newViewModel()
        viewModel.onAction(DeleteAccountAction.DeleteClicked)

        viewModel.onAction(DeleteAccountAction.DismissClicked)

        assertFalse(viewModel.state.value.isOpen)
        assertNotNull(users.current)
    }

    /** Escenario: "El Usuario elimina su cuenta sin reservas activas". */
    @Test
    fun `confirming without active bookings deletes the account ends the session and finishes`() =
        runTest(testDispatcher) {
            bookings.seed(booking("done", BookingStatus.COMPLETED))
            val viewModel = newViewModel()
            viewModel.onAction(DeleteAccountAction.DeleteClicked)

            val event = eventAfter(viewModel.events) { viewModel.onAction(DeleteAccountAction.ConfirmClicked) }

            assertEquals(DeleteAccountEvent.AccountDeleted, event)
            assertNull(users.current)
            assertTrue((users.getUser("u1") as DomainResult.Success).value.isDeleted)
            assertFalse(viewModel.state.value.isOpen)
        }

    /** Escenario: "El Usuario intenta eliminar su cuenta con reservas activas" — as Cliente. */
    @Test
    fun `confirming with an active booking as customer is refused and lists it`() = runTest(testDispatcher) {
        bookings.seed(booking("pending", BookingStatus.REQUESTED, providerId = "profile-other"))
        bookings.seed(booking("done", BookingStatus.COMPLETED, providerId = "profile-other"))
        val viewModel = newViewModel()
        viewModel.onAction(DeleteAccountAction.DeleteClicked)

        viewModel.onAction(DeleteAccountAction.ConfirmClicked)
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(listOf("pending"), state.blocked?.asCustomer?.map { it.id })
        assertTrue(state.blocked?.asProvider.orEmpty().isEmpty())
        assertEquals("Manicure gel", state.serviceTitles["service-1"])
        assertFalse(state.isConfirming)
        assertFalse(state.isDeleting)
        assertNotNull(users.current)
        assertFalse(users.current?.isDeleted ?: true)
    }

    /** Escenario: "...con reservas activas" — as Proveedor, and the refusal is not an error. */
    @Test
    fun `confirming with an active booking as provider is refused and lists it`() = runTest(testDispatcher) {
        profiles.seed(ProviderProfile(id = "profile-1", userId = "u1"))
        bookings.seed(booking("confirmed", BookingStatus.CONFIRMED, customerId = "someone"))
        val viewModel = newViewModel()
        viewModel.onAction(DeleteAccountAction.DeleteClicked)

        viewModel.onAction(DeleteAccountAction.ConfirmClicked)
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(listOf("confirmed"), state.blocked?.asProvider?.map { it.id })
        assertNull(state.error)
        assertNotNull(users.current)
    }

    @Test
    fun `dismissing the list of blocking bookings closes it`() = runTest(testDispatcher) {
        bookings.seed(booking("pending", BookingStatus.REQUESTED, providerId = "profile-other"))
        val viewModel = newViewModel()
        viewModel.onAction(DeleteAccountAction.DeleteClicked)
        viewModel.onAction(DeleteAccountAction.ConfirmClicked)
        advanceUntilIdle()

        viewModel.onAction(DeleteAccountAction.DismissClicked)

        assertFalse(viewModel.state.value.isOpen)
        assertNull(viewModel.state.value.blocked)
    }

    @Test
    fun `a failure while deleting is an error and keeps the confirmation open`() = runTest(testDispatcher) {
        users.signOut()
        val viewModel = newViewModel()
        viewModel.onAction(DeleteAccountAction.DeleteClicked)

        viewModel.onAction(DeleteAccountAction.ConfirmClicked)
        advanceUntilIdle()

        assertNotNull(viewModel.state.value.error)
        assertTrue(viewModel.state.value.isConfirming)
        assertFalse(viewModel.state.value.isDeleting)
    }

    @Test
    fun `entering again closes whatever a previous visit left open`() = runTest(testDispatcher) {
        val viewModel = newViewModel()
        viewModel.onAction(DeleteAccountAction.DeleteClicked)

        viewModel.onAction(DeleteAccountAction.Start)

        assertEquals(DeleteAccountUiState(), viewModel.state.value)
    }
}
