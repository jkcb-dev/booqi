package corp.khin.solutions.booqi.domain.repository

import corp.khin.solutions.booqi.core.common.DomainResult
import corp.khin.solutions.booqi.domain.model.Booking
import corp.khin.solutions.booqi.domain.model.BookingDraft
import corp.khin.solutions.booqi.domain.model.BookingStatus
import kotlin.time.Instant

/**
 * Domain-owned contract for the `Booking` aggregate (docs/domain/provider-flow.md § Grupo 4 —
 * Gestión de Reservas y Calificaciones; shared with the Customer side, #25). The implementation
 * (in `data`) decides how bookings are sourced/persisted — this interface is the only thing a use
 * case is allowed to know about.
 *
 * It is deliberately a plain store: **state-machine rules live on
 * [corp.khin.solutions.booqi.domain.model.Booking]**, not here. A use case loads a Booking, asks it
 * to transition, and hands the result to [updateBooking].
 *
 * Ordering contracts are guaranteed by the implementation (a real datasource may return rows in
 * any order); every list method yields an empty list, never `NotFound`, when nothing matches.
 */
interface BookingRepository {

    /**
     * Escenario: "Un Cliente solicita una reserva". Persists a new Booking in
     * [corp.khin.solutions.booqi.domain.model.BookingStatus.REQUESTED] for [draft]; the id is
     * assigned by storage. Callers must have validated the request (see
     * [corp.khin.solutions.booqi.domain.usecase.SolicitarReservaUseCase]).
     *
     * Not a guarantee against double booking by itself: the use case checks availability first,
     * but two concurrent requests for one slot can both pass that check. Closing that race is the
     * real backend's job (#27 — e.g. an exclusion constraint on provider + time range for active
     * statuses); the in-memory fake is single-writer.
     */
    suspend fun createBooking(draft: BookingDraft): DomainResult<Booking>

    /** The Booking identified by [bookingId], or `NotFound`. */
    suspend fun getBooking(bookingId: String): DomainResult<Booking>

    /**
     * Persists the new state of an existing [booking] (after a transition or a rating), replacing
     * the stored one with the same id; `NotFound` if there is none. The #27 implementation should
     * make this conditional on the stored status so two concurrent transitions cannot both win.
     */
    suspend fun updateBooking(booking: Booking): DomainResult<Booking>

    /**
     * Every Booking of [providerId] whose status is in [statuses] (all statuses when `null`),
     * ordered by [Booking.scheduledAt] ascending, then by id — a stable agenda order.
     */
    suspend fun getBookingsByProvider(
        providerId: String,
        statuses: Set<BookingStatus>? = null,
    ): DomainResult<List<Booking>>

    /**
     * Every [BookingStatus.REQUESTED] Booking, across all Providers, with
     * `requestedAt <= cutoff` — the candidates for expiry. Oldest request first, then by id.
     */
    suspend fun getPendingRequestedAtOrBefore(cutoff: Instant): DomainResult<List<Booking>>

    /**
     * Every Booking of [providerId] that has a rating, newest first: by [Booking.completedAt]
     * descending (a rating has no timestamp of its own; it is left after completion), then
     * [Booking.scheduledAt] descending, then id.
     */
    suspend fun getRatedBookingsByProvider(providerId: String): DomainResult<List<Booking>>

    /**
     * Every Booking requested by [customerId] (`Booking.customerId`) whose status is in
     * [statuses] (all statuses when `null`), ordered by [Booking.scheduledAt] ascending, then by
     * id. Used by account deletion (identity-flow.md): the Customer-side active Bookings that
     * block it, and the history to scrub. Empty list when nothing matches.
     */
    suspend fun getBookingsByCustomer(
        customerId: String,
        statuses: Set<BookingStatus>? = null,
    ): DomainResult<List<Booking>>
}
