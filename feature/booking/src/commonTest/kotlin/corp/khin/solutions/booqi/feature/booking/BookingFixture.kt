package corp.khin.solutions.booqi.feature.booking

import corp.khin.solutions.booqi.domain.model.Address
import corp.khin.solutions.booqi.domain.model.Availability
import corp.khin.solutions.booqi.domain.model.Booking
import corp.khin.solutions.booqi.domain.model.BookingStatus
import corp.khin.solutions.booqi.domain.model.DayHours
import corp.khin.solutions.booqi.domain.model.ProviderProfile
import corp.khin.solutions.booqi.domain.model.Service
import corp.khin.solutions.booqi.domain.model.ServiceModality
import corp.khin.solutions.booqi.domain.model.TimeRange
import corp.khin.solutions.booqi.domain.model.TimeSlot
import corp.khin.solutions.booqi.domain.model.User
import corp.khin.solutions.booqi.domain.usecase.CalificarCitaUseCase
import corp.khin.solutions.booqi.domain.usecase.CancelarReservaClienteUseCase
import corp.khin.solutions.booqi.domain.usecase.GenerarTimeSlotsUseCase
import corp.khin.solutions.booqi.domain.usecase.ObtenerCalificacionesDelProveedorUseCase
import corp.khin.solutions.booqi.domain.usecase.ObtenerHorarioUseCase
import corp.khin.solutions.booqi.domain.usecase.ObtenerMiDireccionUseCase
import corp.khin.solutions.booqi.domain.usecase.ObtenerReservaUseCase
import corp.khin.solutions.booqi.domain.usecase.ObtenerServicioUseCase
import corp.khin.solutions.booqi.domain.usecase.ObtenerTimeSlotsDisponiblesUseCase
import corp.khin.solutions.booqi.domain.usecase.ObtenerUsuarioActualUseCase
import corp.khin.solutions.booqi.domain.usecase.RecalcularCalificacionDelProveedorUseCase
import corp.khin.solutions.booqi.domain.usecase.RequerirCuentaVerificadaUseCase
import corp.khin.solutions.booqi.domain.usecase.SolicitarReservaUseCase
import corp.khin.solutions.booqi.domain.usecase.VerDetalleServicioUseCase
import corp.khin.solutions.booqi.domain.usecase.VerHistorialReservasUseCase
import corp.khin.solutions.booqi.domain.usecase.VerPerfilProveedorUseCase
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant

/** A [Clock] the test moves by hand. */
class TestClock(var instant: Instant) : Clock {
    override fun now(): Instant = instant
}

/**
 * One Provider ("Studio Booqi", profile `p1`) who works Mondays 09:00 to 13:00, two Services (a
 * Local "Corte" `s-local`, a Domicilio "Limpieza" `s-home` and a Local & Dom. "Masaje" `s-both`),
 * a verified signed-in Customer, and the real use cases composed over the fakes the way
 * `domainModule` composes them. Time is fixed at Monday 5 October 2026 11:00 UTC (the tests use
 * [TimeZone.UTC]), so today's 09:00 and 10:00 slots are in the past and 11:00 is "now" (also past:
 * a slot starting exactly now cannot be requested), leaving 12:00 free.
 */
class BookingFixture {
    val clock = TestClock(NOW)
    val bookings = FakeBookingRepository()
    val users = FakeUserRepository()
    val availability = FakeAvailabilityRepository()
    val profiles = FakeProviderProfileRepository(listOf(PROVIDER))
    val services = FakeServiceRepository(listOf(LOCAL, HOME, BOTH))
    val customer: User = users.signIn(CUSTOMER)

    private val currentUser = ObtenerUsuarioActualUseCase(users)
    private val availableSlots = ObtenerTimeSlotsDisponiblesUseCase(
        availability, profiles, bookings, GenerarTimeSlotsUseCase(),
    )
    private val lookups = BookingLookups(
        ObtenerServicioUseCase(services),
        VerPerfilProveedorUseCase(profiles, services, ObtenerCalificacionesDelProveedorUseCase(bookings)),
    )

    init {
        availability.seed(
            Availability(
                providerId = PROVIDER.id,
                weeklyHours = listOf(DayHours(DayOfWeek.MONDAY, true, TimeRange(time(9), time(13)))),
            ),
        )
    }

    fun flowViewModel() = BookingFlowViewModel(
        slots = BookingSlotQueries(
            detail = VerDetalleServicioUseCase(services, profiles),
            schedule = ObtenerHorarioUseCase(availability),
            generate = GenerarTimeSlotsUseCase(),
            available = availableSlots,
        ),
        customer = BookingCustomerAccess(
            requireVerifiedAccount = RequerirCuentaVerificadaUseCase(users),
            savedAddress = ObtenerMiDireccionUseCase(users),
        ),
        solicitarReserva = SolicitarReservaUseCase(
            bookings, services, availableSlots, RequerirCuentaVerificadaUseCase(users), clock,
        ),
        clock = clock,
        timeZone = TimeZone.UTC,
    )

    fun detailViewModel() = BookingDetailViewModel(
        obtenerReserva = ObtenerReservaUseCase(bookings),
        cancelarReserva = CancelarReservaClienteUseCase(currentUser, bookings, clock, TimeZone.UTC),
        lookups = lookups,
        clock = clock,
        timeZone = TimeZone.UTC,
    )

    fun myBookingsViewModel() = MyBookingsViewModel(
        verHistorial = VerHistorialReservasUseCase(currentUser, bookings),
        lookups = lookups,
        clock = clock,
    )

    fun rateViewModel() = RateBookingViewModel(
        obtenerReserva = ObtenerReservaUseCase(bookings),
        calificarCita = CalificarCitaUseCase(
            bookings,
            RecalcularCalificacionDelProveedorUseCase(bookings, profiles),
        ),
        lookups = lookups,
    )

    /** A Booking of [CUSTOMER] for [LOCAL] in any state. */
    fun booking(
        id: String,
        status: BookingStatus = BookingStatus.REQUESTED,
        scheduledAt: LocalDateTime = LocalDateTime(2026, 10, 12, 10, 0),
        requestedAt: Instant = NOW - 1.hours,
        customerId: String = CUSTOMER.id,
        serviceId: String = LOCAL.id,
    ): Booking = bookings.seed(
        Booking(
            id = id,
            providerId = PROVIDER.id,
            serviceId = serviceId,
            customerId = customerId,
            scheduledAt = scheduledAt,
            durationMinutesSnapshot = LOCAL.durationMinutes,
            priceCentsSnapshot = LOCAL.priceCents,
            requestedAt = requestedAt,
            status = status,
        ),
    )

    companion object {
        val NOW: Instant = Instant.parse("2026-10-05T11:00:00Z")
        val TODAY: LocalDate = LocalDate(2026, 10, 5)
        val NEXT_MONDAY: LocalDate = LocalDate(2026, 10, 12)

        val CUSTOMER = User("customer-1", "Ana", "ana@example.com", isEmailVerified = true)
        val HOME_ADDRESS = Address("Av. Siempre Viva 742", -34.6, -58.4)

        val PROVIDER = ProviderProfile(
            id = "p1",
            userId = "u-provider",
            name = "Studio Booqi",
            photoUrl = "https://example.test/p1.jpg",
            description = "Atención con reserva previa.",
            location = "Av. Corrientes 1234",
            isComplete = true,
        )

        val LOCAL = service("s-local", "Corte clásico", ServiceModality.LOCAL)
        val HOME = service("s-home", "Limpieza profunda", ServiceModality.DOMICILIO)
        val BOTH = service("s-both", "Masaje", ServiceModality.AMBOS)

        private fun service(id: String, title: String, modality: ServiceModality) = Service(
            id = id,
            providerId = PROVIDER.id,
            title = title,
            photoUrl = "https://example.test/$id.jpg",
            description = "Descripción",
            priceCents = 3500,
            durationMinutes = 60,
            modality = modality,
        )

        fun time(hour: Int, minute: Int = 0) = LocalTime(hour, minute)
        fun slot(date: LocalDate, hour: Int) = TimeSlot(PROVIDER.id, date, time(hour))
    }
}
