package corp.khin.solutions.booqi.data.repository

import corp.khin.solutions.booqi.core.common.DomainError
import corp.khin.solutions.booqi.core.common.DomainResult
import corp.khin.solutions.booqi.data.datasource.FakeAuthRemoteDataSource
import corp.khin.solutions.booqi.data.datasource.FakeBookingRemoteDataSource
import corp.khin.solutions.booqi.data.datasource.FakeProviderProfileRemoteDataSource
import corp.khin.solutions.booqi.data.datasource.FakeServiceRemoteDataSource
import corp.khin.solutions.booqi.data.datasource.SampleData
import corp.khin.solutions.booqi.domain.model.AccountDeletionResult
import corp.khin.solutions.booqi.domain.model.Credentials
import corp.khin.solutions.booqi.domain.model.Registration
import corp.khin.solutions.booqi.domain.usecase.EliminarCuentaUseCase
import corp.khin.solutions.booqi.domain.usecase.IniciarSesionUseCase
import corp.khin.solutions.booqi.domain.usecase.ObtenerCalificacionesDelProveedorUseCase
import corp.khin.solutions.booqi.domain.usecase.ObtenerMiPerfilDeProveedorUseCase
import corp.khin.solutions.booqi.domain.usecase.ObtenerReservasDelProveedorUseCase
import corp.khin.solutions.booqi.domain.usecase.ObtenerServiciosDelProveedorUseCase
import corp.khin.solutions.booqi.domain.usecase.ObtenerUsuarioPublicoUseCase
import corp.khin.solutions.booqi.domain.usecase.RegistrarUsuarioUseCase
import corp.khin.solutions.booqi.domain.usecase.RequerirCuentaVerificadaUseCase
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * [SampleData] under the providerId contract of #56/#58: what the app seeds on a device — the demo
 * account, its ProviderProfile and the records keyed by that profile's id — joins up end to end.
 */
class SampleDataIdentityTest {

    private val auth = FakeAuthRemoteDataSource(seed = SampleData.accounts())
    private val users = UserRepositoryImpl(auth)
    private val profiles = ProviderProfileRepositoryImpl(
        FakeProviderProfileRemoteDataSource(seed = SampleData.providerProfiles()),
    )
    private val services = ServiceRepositoryImpl(FakeServiceRemoteDataSource(seed = SampleData.services()))
    private val bookings = BookingRepositoryImpl(FakeBookingRemoteDataSource(seed = SampleData.bookings()))

    private fun <T> DomainResult<T>.ok(): T = (this as DomainResult.Success<T>).value

    private suspend fun signInDemo() =
        IniciarSesionUseCase(users)(Credentials.WithEmail(SampleData.DEMO_EMAIL, SampleData.DEMO_PASSWORD)).ok()

    @Test
    fun `the app starts signed out`() = runTest {
        assertEquals(null, users.getCurrentUser().ok())
    }

    @Test
    fun `the demo account signs in with its documented credentials and may book and activate provider mode`() =
        runTest {
            val demo = signInDemo()

            assertEquals(SampleData.DEMO_USER_ID, demo.id)
            assertTrue(demo.isEmailVerified)
            assertEquals(demo, RequerirCuentaVerificadaUseCase(users)().ok())
        }

    @Test
    fun `the demo account does not accept another password`() = runTest {
        val result = IniciarSesionUseCase(users)(Credentials.WithEmail(SampleData.DEMO_EMAIL, "otra-clave-123"))

        assertEquals(DomainResult.Failure(DomainError.Unauthorized), result)
    }

    @Test
    fun `the demo email cannot be registered again`() = runTest {
        val result = RegistrarUsuarioUseCase(users)(Registration.WithEmail("Otra", SampleData.DEMO_EMAIL, "secreto123"))

        assertIs<DomainResult.Failure>(result)
    }

    @Test
    fun `the demo provider id is its profile id and the sample services and bookings are keyed by it`() = runTest {
        signInDemo()

        val profile = ObtenerMiPerfilDeProveedorUseCase(users, profiles)().ok()

        assertEquals(SampleData.DEMO_PROVIDER_PROFILE_ID, profile?.id)
        assertEquals(SampleData.DEMO_USER_ID, profile?.userId)
        val ownServices = ObtenerServiciosDelProveedorUseCase(services)(SampleData.DEMO_PROVIDER_PROFILE_ID).ok()
        assertEquals(setOf("Manicure gel", "Corte a domicilio"), ownServices.map { it.title }.toSet())
        assertTrue(ObtenerReservasDelProveedorUseCase(bookings)(SampleData.DEMO_PROVIDER_PROFILE_ID).ok().isNotEmpty())
    }

    @Test
    fun `the sample customers have real names and a deleted one reads Usuario eliminado`() = runTest {
        val names = ObtenerUsuarioPublicoUseCase(users)
        val reviews = ObtenerCalificacionesDelProveedorUseCase(bookings)(SampleData.DEMO_PROVIDER_PROFILE_ID).ok()

        val authors = reviews.map { names(it.customerId).ok().displayName }

        assertEquals(setOf("Camila Torres", "Usuario eliminado"), authors.toSet())
    }

    @Test
    fun `deleting the demo account is refused while it has active bookings as provider`() = runTest {
        signInDemo()

        val result = EliminarCuentaUseCase(users, bookings, profiles)().ok()

        val blocked = assertIs<AccountDeletionResult.Blocked>(result)
        assertTrue(blocked.activeBookings.asProvider.isNotEmpty())
        assertTrue(blocked.activeBookings.asCustomer.isEmpty())
    }
}
