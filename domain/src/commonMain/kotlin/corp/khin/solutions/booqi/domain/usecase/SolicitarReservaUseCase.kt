package corp.khin.solutions.booqi.domain.usecase

import corp.khin.solutions.booqi.core.common.DomainError
import corp.khin.solutions.booqi.core.common.DomainResult
import corp.khin.solutions.booqi.core.common.asFailure
import corp.khin.solutions.booqi.core.common.flatMap
import corp.khin.solutions.booqi.domain.model.Address
import corp.khin.solutions.booqi.domain.model.Booking
import corp.khin.solutions.booqi.domain.model.BookingDraft
import corp.khin.solutions.booqi.domain.model.DateRange
import corp.khin.solutions.booqi.domain.model.Service
import corp.khin.solutions.booqi.domain.model.ServiceModality
import corp.khin.solutions.booqi.domain.model.TimeSlot
import corp.khin.solutions.booqi.domain.repository.BookingRepository
import corp.khin.solutions.booqi.domain.repository.ServiceRepository
import kotlin.time.Clock
import kotlinx.datetime.LocalDateTime

/**
 * Escenario: "Un Cliente solicita una reserva" (docs/domain/provider-flow.md § Grupo 4; reused by
 * the Customer side, #25). Creates a Booking in REQUESTED; the Provider then has
 * [Booking.RESPONSE_WINDOW] (24h) to answer, and the slot stops being available to others while it
 * is pending (see [ObtenerTimeSlotsDisponiblesUseCase]).
 *
 * Validation, all before anything is stored:
 * - the Service must exist (`NotFound` otherwise), be active, and belong to the [slot]'s Provider
 *   — else `InvalidInput`;
 * - [deliveryAddress] is **required** when the Service modality is DOMICILIO and **dropped** for
 *   LOCAL; for AMBOS it is stored as given (the Customer may or may not have chosen delivery);
 * - [slot] must be in the Provider's currently available slots for this Service's duration
 *   (schedule, pause, blocks and other active Bookings considered) — else `InvalidInput`.
 *
 * The Booking **snapshots** the Service's price and duration and the delivery address (never live
 * references, docs/DOMAIN.md); a later edit of the Service doesn't change it. The Provider's
 * notification is deferred (no notification system yet). Check-then-insert is not atomic — see
 * [BookingRepository.createBooking] for how the real backend closes the race. [clock] exists so
 * tests can fake time.
 */
class SolicitarReservaUseCase(
    private val bookings: BookingRepository,
    private val services: ServiceRepository,
    private val availableSlots: ObtenerTimeSlotsDisponiblesUseCase,
    private val clock: Clock = Clock.System,
) {
    suspend operator fun invoke(
        customerId: String,
        serviceId: String,
        slot: TimeSlot,
        deliveryAddress: Address? = null,
        customerNote: String? = null,
    ): DomainResult<Booking> =
        services.getService(serviceId).flatMap { service ->
            validate(service, slot, deliveryAddress)?.asFailure()
                ?: createIfAvailable(slot, draft(service, customerId, slot, deliveryAddress, customerNote))
        }

    // The draft carries the snapshots; its duration is the Service's, which is what the slot must fit.
    private suspend fun createIfAvailable(slot: TimeSlot, draft: BookingDraft): DomainResult<Booking> =
        availableSlots(slot.providerId, draft.durationMinutesSnapshot, DateRange(slot.date, slot.date))
            .flatMap { open ->
                if (slot in open) {
                    bookings.createBooking(draft)
                } else {
                    DomainError.InvalidInput("El horario seleccionado ya no está disponible").asFailure()
                }
            }

    private fun validate(service: Service, slot: TimeSlot, deliveryAddress: Address?): DomainError.InvalidInput? =
        when {
            !service.isActive -> DomainError.InvalidInput("El servicio no está disponible")
            service.providerId != slot.providerId ->
                DomainError.InvalidInput("El horario no pertenece al Proveedor de este servicio")
            service.modality == ServiceModality.DOMICILIO && deliveryAddress == null ->
                DomainError.InvalidInput("La dirección es obligatoria para un servicio a domicilio")
            else -> null
        }

    private fun draft(
        service: Service,
        customerId: String,
        slot: TimeSlot,
        deliveryAddress: Address?,
        customerNote: String?,
    ) = BookingDraft(
        providerId = service.providerId,
        serviceId = service.id,
        customerId = customerId,
        scheduledAt = LocalDateTime(slot.date, slot.start),
        durationMinutesSnapshot = service.durationMinutes,
        priceCentsSnapshot = service.priceCents,
        requestedAt = clock.now(),
        deliveryAddress = deliveryAddress.takeUnless { service.modality == ServiceModality.LOCAL },
        customerNote = customerNote?.trim()?.takeIf { it.isNotEmpty() },
    )
}
