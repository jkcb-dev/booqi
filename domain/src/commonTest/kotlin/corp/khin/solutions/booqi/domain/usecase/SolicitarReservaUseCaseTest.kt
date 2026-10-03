package corp.khin.solutions.booqi.domain.usecase

import corp.khin.solutions.booqi.domain.model.Address
import corp.khin.solutions.booqi.domain.model.BlockedPeriod
import corp.khin.solutions.booqi.domain.model.BookingStatus
import corp.khin.solutions.booqi.domain.model.DateRange
import corp.khin.solutions.booqi.domain.model.ProviderReasonCode
import corp.khin.solutions.booqi.domain.model.ServiceDetails
import corp.khin.solutions.booqi.domain.model.ServiceModality
import corp.khin.solutions.booqi.domain.model.TimeSlot
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.plus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.hours

/**
 * Escenario: "Un Cliente solicita una reserva" (docs/domain/provider-flow.md § Grupo 4) — a
 * Booking in "Requested", the slot taken, 24h for the Provider — plus the snapshot rule
 * (docs/DOMAIN.md), the Domicilio address rule, double booking and the rejected-request paths.
 * The Provider's notification is deferred and not asserted.
 */
class SolicitarReservaUseCaseTest {

    private val f = SchedulingFixture()
    private val home = Address("Calle Falsa 123", 40.4, -3.7)

    private suspend fun ready(modality: ServiceModality = ServiceModality.LOCAL, duration: Int = 60) =
        f.givenProvider().let { f.givenService(modality, duration) }

    @Test
    fun `requesting an available slot creates a requested booking with a 24 hour deadline`() = runTest {
        val service = ready()

        val booking = f.solicitar("customer-1", service.id, f.slot(10)).value()

        assertEquals(BookingStatus.REQUESTED, booking.status)
        assertEquals(f.providerId, booking.providerId)
        assertEquals(service.id, booking.serviceId)
        assertEquals("customer-1", booking.customerId)
        assertEquals(LocalDateTime(MONDAY_DATE, time(10)), booking.scheduledAt)
        assertEquals(f.clock.now, booking.requestedAt)
        assertEquals(f.clock.now + 24.hours, booking.responseDeadline)
        assertEquals(booking, f.bookings.stored(booking.id))
    }

    @Test
    fun `the requested slot is no longer available to other customers while pending`() = runTest {
        val service = ready()
        f.solicitar("customer-1", service.id, f.slot(10)).value()

        val open = f.availableSlots(f.providerId, 60, DateRange(MONDAY_DATE, MONDAY_DATE)).value()

        assertEquals(listOf(f.slot(9), f.slot(11)), open)
    }

    @Test
    fun `a second request for the same slot is rejected as no longer available`() = runTest {
        val service = ready()
        f.solicitar("customer-1", service.id, f.slot(10)).value()

        val message = f.solicitar("customer-2", service.id, f.slot(10)).invalidInput()

        assertEquals("El horario seleccionado ya no está disponible", message)
        assertEquals(1, f.bookings.writeCount)
    }

    @Test
    fun `a slot can be requested again after the first request was rejected`() = runTest {
        val service = ready()
        val first = f.solicitar("customer-1", service.id, f.slot(10)).value()
        RechazarReservaUseCase(f.bookings, f.clock)(first.id, ProviderReasonCode.OTHER).value()

        val second = f.solicitar("customer-2", service.id, f.slot(10)).value()

        assertEquals("customer-2", second.customerId)
    }

    @Test
    fun `a request that overlaps another service's booking is rejected`() = runTest {
        val long = ready(duration = 120)
        val short = f.givenService(durationMinutes = 60)
        f.solicitar("customer-1", long.id, f.slot(9)).value() // holds 09:00-11:00

        f.solicitar("customer-2", short.id, f.slot(10)).invalidInput()
        f.solicitar("customer-2", short.id, f.slot(11)).value()
    }

    // --- Snapshots ---

    @Test
    fun `price and duration are copied from the service at request time`() = runTest {
        val service = ready(duration = 45)
        val booking = f.solicitar("customer-1", service.id, f.slot(9)).value()

        assertEquals(45, booking.durationMinutesSnapshot)
        assertEquals(3500, booking.priceCentsSnapshot)
    }

    @Test
    fun `editing the service afterwards does not change the booking`() = runTest {
        val service = ready()
        val booking = f.solicitar("customer-1", service.id, f.slot(9)).value()

        EditarServicioUseCase(f.services)(
            service.id,
            ServiceDetails("Corte premium", "https://example.com/p.jpg", "Nuevo", 9900, 90, ServiceModality.LOCAL),
        ).value()

        val stored = f.bookings.stored(booking.id)
        assertEquals(3500, stored.priceCentsSnapshot)
        assertEquals(60, stored.durationMinutesSnapshot)
    }

    @Test
    fun `a disabled service afterwards does not affect the booking`() = runTest {
        val service = ready()
        val booking = f.solicitar("customer-1", service.id, f.slot(9)).value()

        DeshabilitarServicioUseCase(f.services)(service.id).value()

        assertEquals(BookingStatus.REQUESTED, f.bookings.stored(booking.id).status)
    }

    // --- Delivery address ---

    @Test
    fun `a Domicilio service requires a delivery address`() = runTest {
        val service = ready(ServiceModality.DOMICILIO)

        f.solicitar("customer-1", service.id, f.slot(9)).invalidInput()

        assertEquals(0, f.bookings.writeCount)
    }

    @Test
    fun `a Domicilio service stores the address snapshot`() = runTest {
        val service = ready(ServiceModality.DOMICILIO)

        val booking = f.solicitar("customer-1", service.id, f.slot(9), deliveryAddress = home).value()

        assertEquals(home, booking.deliveryAddress)
    }

    @Test
    fun `a Local service ignores any address it was given`() = runTest {
        val service = ready(ServiceModality.LOCAL)

        val booking = f.solicitar("customer-1", service.id, f.slot(9), deliveryAddress = home).value()

        assertNull(booking.deliveryAddress)
    }

    @Test
    fun `an Ambos service stores the address when given and works without one`() = runTest {
        val service = ready(ServiceModality.AMBOS)

        val withAddress = f.solicitar("customer-1", service.id, f.slot(9), deliveryAddress = home).value()
        val without = f.solicitar("customer-2", service.id, f.slot(10)).value()

        assertEquals(home, withAddress.deliveryAddress)
        assertNull(without.deliveryAddress)
    }

    @Test
    fun `the customer note is kept trimmed and a blank one is dropped`() = runTest {
        val service = ready()

        val noted = f.solicitar("customer-1", service.id, f.slot(9), customerNote = "  Timbre roto ").value()
        val blank = f.solicitar("customer-2", service.id, f.slot(10), customerNote = "  ").value()

        assertEquals("Timbre roto", noted.customerNote)
        assertNull(blank.customerNote)
    }

    // --- Rejected requests ---

    @Test
    fun `an unknown service is NotFound`() = runTest {
        f.givenProvider()

        f.solicitar("customer-1", "ghost", f.slot(9)).assertNotFound()
    }

    @Test
    fun `a disabled service cannot be booked`() = runTest {
        val service = ready()
        DeshabilitarServicioUseCase(f.services)(service.id).value()

        f.solicitar("customer-1", service.id, f.slot(9)).invalidInput()

        assertEquals(0, f.bookings.writeCount)
    }

    @Test
    fun `a slot of another provider cannot be used with this service`() = runTest {
        val service = ready()

        f.solicitar("customer-1", service.id, TimeSlot("other-provider", MONDAY_DATE, time(9))).invalidInput()

        assertEquals(0, f.bookings.writeCount)
    }

    @Test
    fun `a slot outside the weekly hours is rejected`() = runTest {
        val service = ready()

        f.solicitar("customer-1", service.id, f.slot(14)).invalidInput() // after 12:00
        f.solicitar("customer-1", service.id, f.slot(9, 30)).invalidInput() // off the 60-minute grid
        f.solicitar("customer-1", service.id, TimeSlot(f.providerId, MONDAY_DATE.plus(1, DateTimeUnit.DAY), time(9)))
            .invalidInput() // Tuesday, no schedule

        assertEquals(0, f.bookings.writeCount)
    }

    @Test
    fun `a blocked slot is rejected`() = runTest {
        val service = ready()
        f.availability.addBlockedPeriod(f.providerId, BlockedPeriod(MONDAY_DATE, range(10, 11)))

        f.solicitar("customer-1", service.id, f.slot(10)).invalidInput()
        f.solicitar("customer-1", service.id, f.slot(9)).value()
    }

    @Test
    fun `a slot during the paused range is rejected`() = runTest {
        val service = ready()
        f.profiles.setPausedRange(f.providerId, DateRange(MONDAY_DATE, MONDAY_DATE))

        f.solicitar("customer-1", service.id, f.slot(9)).invalidInput()
    }
}
