package corp.khin.solutions.booqi.domain.usecase

import corp.khin.solutions.booqi.domain.model.Service
import corp.khin.solutions.booqi.domain.model.ServiceDetails
import corp.khin.solutions.booqi.domain.model.ServiceModality
import corp.khin.solutions.booqi.domain.model.TimeSlot
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
    val solicitar = SolicitarReservaUseCase(bookings, services, availableSlots, clock)

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
