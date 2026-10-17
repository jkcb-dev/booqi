package corp.khin.solutions.booqi.domain.usecase

import corp.khin.solutions.booqi.core.common.DomainResult
import corp.khin.solutions.booqi.domain.model.Address
import corp.khin.solutions.booqi.domain.model.Booking
import corp.khin.solutions.booqi.domain.model.Service
import corp.khin.solutions.booqi.domain.model.ServiceDetails
import corp.khin.solutions.booqi.domain.model.ServiceModality
import corp.khin.solutions.booqi.domain.model.TimeSlot
import corp.khin.solutions.booqi.domain.model.User
import kotlinx.datetime.DayOfWeek

/**
 * A wired-up set of fakes for the Booking-request and availability tests: one Provider who works
 * Mondays 09:00-12:00 ([MONDAY_DATE] is such a Monday), the use cases composed the way
 * `domainModule` composes them, and a [FakeClock] starting at [REQUESTED_AT].
 */
internal class SchedulingFixture {
    val clock = FakeClock(REQUESTED_AT)
    val profiles = FakeProviderProfileRepository()
    val availability = FakeAvailabilityRepository()
    val services = FakeServiceRepository()
    val bookings = FakeBookingRepository()

    val availableSlots = ObtenerTimeSlotsDisponiblesUseCase(availability, profiles, bookings, GenerarTimeSlotsUseCase())
    val users = FakeUserRepository()

    /** The real use case, resolving the signed-in user itself: arrange the session with [signInCustomer]. */
    val solicitarUseCase = SolicitarReservaUseCase(
        bookings, services, availableSlots, RequerirCuentaVerificadaUseCase(users), clock,
    )

    /** Signs in a verified Customer; [name] is what the booking's name snapshot will copy. */
    fun signInCustomer(id: String = "customer-1", name: String = "Cliente $id"): User =
        users.seedSignedIn(User(id, name, "$id@example.com", isEmailVerified = true))

    /**
     * Requests as [customerId]: signs that Customer in first, then calls [solicitarUseCase], so the
     * request tests can switch customers freely. The guard tests call [solicitarUseCase] directly.
     */
    val solicitar = SolicitarAsCustomer()

    inner class SolicitarAsCustomer {
        suspend operator fun invoke(
            customerId: String,
            serviceId: String,
            slot: TimeSlot,
            deliveryAddress: Address? = null,
            customerNote: String? = null,
        ): DomainResult<Booking> {
            signInCustomer(customerId)
            return solicitarUseCase(customerId, serviceId, slot, deliveryAddress, customerNote)
        }
    }

    /** The provider id (= ProviderProfile.id), set up by [givenProvider]. */
    lateinit var providerId: String
        private set

    /** Activates a Provider who works Mondays from [startHour] to [endHour]. */
    suspend fun givenProvider(startHour: Int = 9, endHour: Int = 12): String {
        providerId = profiles.activateProviderMode("user-1").value().id
        availability.saveWeeklyHours(providerId, listOf(workday(DayOfWeek.MONDAY, startHour, endHour)))
        return providerId
    }

    suspend fun givenService(
        modality: ServiceModality = ServiceModality.LOCAL,
        durationMinutes: Int = 60,
        priceCents: Int = 3500,
    ): Service = services.addService(
        providerId,
        ServiceDetails("Corte", "https://example.com/p.jpg", "Corte clásico", priceCents, durationMinutes, modality),
    ).value()

    fun slot(hour: Int, minute: Int = 0) = TimeSlot(providerId, MONDAY_DATE, time(hour, minute))
}
