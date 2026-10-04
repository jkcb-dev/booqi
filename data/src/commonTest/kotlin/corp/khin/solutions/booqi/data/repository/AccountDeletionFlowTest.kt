package corp.khin.solutions.booqi.data.repository

import corp.khin.solutions.booqi.core.common.DomainError
import corp.khin.solutions.booqi.core.common.DomainResult
import corp.khin.solutions.booqi.data.datasource.FakeAuthRemoteDataSource
import corp.khin.solutions.booqi.data.datasource.FakeBookingRemoteDataSource
import corp.khin.solutions.booqi.data.datasource.FakeProviderProfileRemoteDataSource
import corp.khin.solutions.booqi.data.datasource.FakeServiceRemoteDataSource
import corp.khin.solutions.booqi.data.dto.BookingDto
import corp.khin.solutions.booqi.data.dto.ServiceDto
import corp.khin.solutions.booqi.domain.model.AccountDeletionResult
import corp.khin.solutions.booqi.domain.model.ProviderProfile
import corp.khin.solutions.booqi.domain.model.Registration
import corp.khin.solutions.booqi.domain.model.User
import corp.khin.solutions.booqi.domain.usecase.ActivarModoProveedorUseCase
import corp.khin.solutions.booqi.domain.usecase.BuscarServiciosUseCase
import corp.khin.solutions.booqi.domain.usecase.CompletarPerfilDeProveedorUseCase
import corp.khin.solutions.booqi.domain.usecase.EliminarCuentaUseCase
import corp.khin.solutions.booqi.domain.usecase.ObtenerCalificacionesDelProveedorUseCase
import corp.khin.solutions.booqi.domain.usecase.ObtenerMiPerfilDeProveedorUseCase
import corp.khin.solutions.booqi.domain.usecase.ObtenerUsuarioPublicoUseCase
import corp.khin.solutions.booqi.domain.usecase.RegistrarUsuarioUseCase
import corp.khin.solutions.booqi.domain.usecase.RequerirCuentaVerificadaUseCase
import corp.khin.solutions.booqi.domain.usecase.VerPerfilProveedorUseCase
import corp.khin.solutions.booqi.domain.usecase.VerificarCorreoUseCase
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * The real repositories and use cases over the TEMPORARY fakes, end to end for the last Identity
 * scenario: deleting an account hides a Provider from search and public pages, keeps their rating
 * and the reviews they received, and leaves other people's history showing "Usuario eliminado".
 */
class AccountDeletionFlowTest {

    private val auth = FakeAuthRemoteDataSource()
    private val users = UserRepositoryImpl(auth)
    private val profileSource = FakeProviderProfileRemoteDataSource()
    private val profiles = ProviderProfileRepositoryImpl(profileSource)
    private val serviceSource = FakeServiceRemoteDataSource()
    private val bookingSource = FakeBookingRemoteDataSource()
    private val bookings = BookingRepositoryImpl(bookingSource)

    private val registrar = RegistrarUsuarioUseCase(users)
    private val eliminar = EliminarCuentaUseCase(users, bookings, profiles)
    private val buscar = BuscarServiciosUseCase(CatalogRepositoryImpl(serviceSource, profileSource))

    private fun <T> DomainResult<T>.ok(): T = (this as DomainResult.Success<T>).value

    private suspend fun signUp(name: String, email: String): User {
        auth.signOut()
        return registrar(Registration.WithEmail(name, email, "secreto123")).ok()
    }

    /** A completed Booking rated 5 stars by [customerId] for [providerId] (a ProviderProfile id). */
    private suspend fun ratedBooking(id: String, customerId: String, providerId: String) = bookingSource.save(
        BookingDto(
            id = id,
            providerId = providerId,
            serviceId = "s1",
            customerId = customerId,
            scheduledAt = "2026-10-01T10:00",
            durationMinutesSnapshot = 60,
            priceCentsSnapshot = 3500,
            deliveryAddressLineSnapshot = "Av. Corrientes 1234",
            deliveryAddressLatSnapshot = -34.6,
            deliveryAddressLngSnapshot = -58.4,
            customerNote = "Piso 3",
            status = "Completed",
            reasonCode = null,
            reasonNote = null,
            ratingStars = 5,
            ratingComment = "Excelente",
            requestedAt = "2026-09-28T10:00:00Z",
            respondedAt = "2026-09-28T11:00:00Z",
            completedAt = "2026-10-01T11:00:00Z",
        ),
    )

    private suspend fun becomeCompleteProvider(user: User): ProviderProfile {
        val profile = ActivarModoProveedorUseCase(profiles)(user.id).ok()
        CompletarPerfilDeProveedorUseCase(profiles)(
            profile.id,
            "Studio ${user.displayName}",
            "https://example.test/p.jpg",
            "Descripción",
            "Av. Santa Fe 1",
        ).ok()
        serviceSource.save(
            ServiceDto(
                id = "service-${profile.id}",
                providerId = profile.id, // the contract: a ProviderProfile id, not the user id
                title = "Corte",
                photoUrl = "https://example.test/s.jpg",
                description = "Corte de pelo",
                priceCents = 3000,
                durationMinutes = 30,
                modality = "local",
                isActive = true,
                category = "barberia",
            ),
        )
        profileSource.save(profileSource.findById(profile.id)!!.copy(ratingAverage = 4.5, ratingCount = 2))
        return profile
    }

    @Test
    fun `a deleted Provider no longer appears in search or on public pages but their rating stays`() = runTest {
        val provider = signUp("Ana", "ana@example.com")
        VerificarCorreoUseCase(users)().ok()
        val profile = becomeCompleteProvider(provider)
        val verPerfil = VerPerfilProveedorUseCase(
            profiles,
            ServiceRepositoryImpl(serviceSource),
            ObtenerCalificacionesDelProveedorUseCase(bookings),
        )
        assertEquals(1, buscar().ok().size) // visible before
        verPerfil(profile.id).ok()

        assertEquals(AccountDeletionResult.Deleted, eliminar().ok())

        assertEquals(emptyList(), buscar().ok())
        assertEquals(DomainError.NotFound, (verPerfil(profile.id) as DomainResult.Failure).error)
        val kept = profileSource.findById(profile.id)!!
        assertEquals(4.5, kept.ratingAverage)
        assertEquals(2, kept.ratingCount)
    }

    @Test
    fun `a deleted Customer leaves their completed booking and rating shown as Usuario eliminado`() = runTest {
        val provider = signUp("Ana", "ana@example.com")
        val profile = becomeCompleteProvider(provider)
        val customer = signUp("Beto", "beto@example.com")
        ratedBooking("booking-1", customer.id, profile.id)
        val before = profileSource.findById(profile.id)!!

        assertEquals(AccountDeletionResult.Deleted, eliminar().ok())

        // The Provider's history and aggregate are untouched...
        val booking = bookings.getBooking("booking-1").ok()
        assertEquals(5, booking.rating?.stars)
        assertEquals("Excelente", booking.rating?.comment)
        assertEquals(before, profileSource.findById(profile.id))
        assertEquals(1, bookings.getRatedBookingsByProvider(profile.id).ok().size)
        // ...the customer's personal data on it is gone, and the name reads "Usuario eliminado".
        assertEquals(null, booking.deliveryAddress)
        assertEquals(null, booking.customerNote)
        val shown = ObtenerUsuarioPublicoUseCase(users)(booking.customerId).ok()
        assertEquals("Usuario eliminado", shown.displayName)
        assertTrue(shown.isDeleted)
        // The Provider is still visible in search.
        assertEquals(1, buscar().ok().size)
    }

    @Test
    fun `a Customer with an active booking cannot delete and nothing changes`() = runTest {
        val provider = signUp("Ana", "ana@example.com")
        val profile = becomeCompleteProvider(provider)
        val customer = signUp("Beto", "beto@example.com")
        ratedBooking("booking-1", customer.id, profile.id)
        bookingSource.save(bookingSource.findById("booking-1")!!.copy(id = "booking-2", status = "Confirmed"))

        val result = assertIs<AccountDeletionResult.Blocked>(eliminar().ok())

        assertEquals(listOf("booking-2"), result.activeBookings.asCustomer.map { it.id })
        assertEquals(customer, users.getCurrentUser().ok())
        assertEquals("Piso 3", bookings.getBooking("booking-1").ok().customerNote)
    }

    @Test
    fun `a Provider with an active booking on their profile cannot delete`() = runTest {
        val provider = signUp("Ana", "ana@example.com")
        val profile = becomeCompleteProvider(provider)
        ratedBooking("booking-1", "someone", profile.id)
        bookingSource.save(bookingSource.findById("booking-1")!!.copy(status = "Requested", ratingStars = null))

        val result = assertIs<AccountDeletionResult.Blocked>(eliminar().ok())

        assertEquals(listOf("booking-1"), result.activeBookings.asProvider.map { it.id })
        assertEquals(1, buscar().ok().size)
        assertEquals(provider, users.getCurrentUser().ok())
    }

    @Test
    fun `the user to profile path and the guard work together`() = runTest {
        val user = signUp("Ana", "ana@example.com")
        val guard = RequerirCuentaVerificadaUseCase(users)
        val miPerfil = ObtenerMiPerfilDeProveedorUseCase(users, profiles)

        assertEquals(null, miPerfil().ok())
        val unverified = (guard() as DomainResult.Failure).error
        assertEquals(DomainError.InvalidInput(RequerirCuentaVerificadaUseCase.EMAIL_NOT_VERIFIED_MESSAGE), unverified)
        VerificarCorreoUseCase(users)().ok()
        val verified = guard().ok()
        val profile = ActivarModoProveedorUseCase(profiles)(verified.id).ok()

        assertEquals(profile, miPerfil().ok())
        assertEquals(user.id, profile.userId)
        assertTrue(profile.id != user.id)
    }
}
