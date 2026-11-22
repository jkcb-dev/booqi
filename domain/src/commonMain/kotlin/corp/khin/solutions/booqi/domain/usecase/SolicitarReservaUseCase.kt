package corp.khin.solutions.booqi.domain.usecase

import corp.khin.solutions.booqi.core.common.DomainError
import corp.khin.solutions.booqi.core.common.DomainResult
import corp.khin.solutions.booqi.core.common.asFailure
import corp.khin.solutions.booqi.core.common.flatMap
import corp.khin.solutions.booqi.domain.model.Address
import corp.khin.solutions.booqi.domain.model.Booking
import corp.khin.solutions.booqi.domain.model.BookingDraft
import corp.khin.solutions.booqi.domain.model.DateRange
import corp.khin.solutions.booqi.domain.model.ProviderProfile
import corp.khin.solutions.booqi.domain.model.Service
import corp.khin.solutions.booqi.domain.model.ServiceModality
import corp.khin.solutions.booqi.domain.model.TimeSlot
import corp.khin.solutions.booqi.domain.model.User
import corp.khin.solutions.booqi.domain.repository.BookingRepository
import corp.khin.solutions.booqi.domain.repository.ProviderProfileRepository
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
 * - [slot] must start **after now** ([ObtenerTimeSlotsDisponiblesUseCase.hasStarted]: injected clock,
 *   device timezone as no per-Provider timezone exists yet) — else `InvalidInput`;
 * - [slot] must be in the Provider's currently available slots for this Service's duration
 *   (schedule, pause, blocks and other active Bookings considered) — else `InvalidInput`.
 *
 * The Booking **snapshots** the Service's price, duration and title, the Provider's name and the
 * delivery address (never live references, docs/DOMAIN.md); a later edit of the Service doesn't
 * change it. The Provider's notification is deferred (no notification system yet).
 * Check-then-insert is not atomic — see [BookingRepository.createBooking] for how the real backend
 * closes the race. The Customer is always the signed-in user (resolved by [requireVerifiedAccount]);
 * there is no `customerId` parameter. [clock] exists so tests can fake time.
 */
class SolicitarReservaUseCase(
    private val bookings: BookingRepository,
    private val services: ServiceRepository,
    private val profiles: ProviderProfileRepository,
    private val availableSlots: ObtenerTimeSlotsDisponiblesUseCase,
    private val requireVerifiedAccount: RequerirCuentaVerificadaUseCase,
    private val clock: Clock = Clock.System,
) {
    suspend operator fun invoke(
        serviceId: String,
        slot: TimeSlot,
        deliveryAddress: Address? = null,
        customerNote: String? = null,
    ): DomainResult<Booking> =
        requireVerifiedAccount().flatMap { user ->
            services.getService(serviceId).flatMap { service ->
                validate(service, slot, deliveryAddress)?.asFailure()
                    ?: profiles.getProfile(service.providerId).flatMap { provider ->
                        val request = Request(slot, deliveryAddress, customerNote)
                        createIfAvailable(slot, draft(service, provider, user, request))
                    }
            }
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
            availableSlots.hasStarted(slot) ->
                DomainError.InvalidInput("El horario seleccionado ya pasó")
            service.providerId != slot.providerId ->
                DomainError.InvalidInput("El horario no pertenece al Proveedor de este servicio")
            service.modality == ServiceModality.DOMICILIO && deliveryAddress == null ->
                DomainError.InvalidInput("La dirección es obligatoria para un servicio a domicilio")
            else -> null
        }

    private fun draft(
        service: Service,
        provider: ProviderProfile,
        customer: User,
        request: Request,
    ) = BookingDraft(
        providerId = service.providerId,
        serviceId = service.id,
        customerId = customer.id,
        scheduledAt = LocalDateTime(request.slot.date, request.slot.start),
        durationMinutesSnapshot = service.durationMinutes,
        priceCentsSnapshot = service.priceCents,
        requestedAt = clock.now(),
        deliveryAddress = request.deliveryAddress.takeUnless { service.modality == ServiceModality.LOCAL },
        customerNote = request.customerNote?.trim()?.takeIf { it.isNotEmpty() },
        customerNameSnapshot = customer.displayName.trim().takeIf { it.isNotEmpty() },
        serviceTitleSnapshot = service.title,
        providerNameSnapshot = provider.name?.trim()?.takeIf { it.isNotEmpty() },
    )

    private class Request(val slot: TimeSlot, val deliveryAddress: Address?, val customerNote: String?)
}
