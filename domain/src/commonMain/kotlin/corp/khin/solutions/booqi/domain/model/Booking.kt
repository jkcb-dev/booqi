package corp.khin.solutions.booqi.domain.model

import corp.khin.solutions.booqi.core.common.DomainError
import corp.khin.solutions.booqi.core.common.DomainResult
import corp.khin.solutions.booqi.core.common.asFailure
import corp.khin.solutions.booqi.core.common.asSuccess
import kotlin.time.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

/**
 * A Customer's request to reserve a Provider's TimeSlot for a Service — the aggregate root of the
 * Scheduling context (docs/DOMAIN.md, docs/domain/provider-flow.md § Grupo 4). Plain data class,
 * no serialization annotations.
 *
 * **providerId contract (#50/#56):** [providerId] is a [ProviderProfile.id], never a `User.id`. Get it
 * from the signed-in user with `ObtenerMiPerfilDeProveedorUseCase` / `ProviderProfileRepository.findByUserId`.
 *
 * **References by ID, snapshots by value.** [providerId], [serviceId] and [customerId] are IDs —
 * a Booking never embeds those aggregates. [priceCentsSnapshot], [durationMinutesSnapshot] and
 * [deliveryAddress] are *copies* taken when the request was made (docs/DATABASE.md `bookings`),
 * so editing the Service or the Customer's saved address later never changes what this Booking
 * says was agreed. [deliveryAddress] is only set for Domicilio appointments.
 *
 * **Time.** [scheduledAt] is a [LocalDateTime]: the Provider's wall-clock date + start time, i.e.
 * [timeSlot]'s `date` + `start` — the same representation `TimeSlot`/`Availability` use (a
 * Provider's "Monday 09:00" is a wall-clock notion, and `docs/DATABASE.md` stores it in a
 * `timestamp` column). There is no timezone feature yet, so no zone is attached; adding one is a
 * future, cross-cutting change to `TimeSlot` too. [requestedAt], [respondedAt] and [completedAt]
 * are real [Instant]s (moments in time, used for the 24h response window).
 *
 * **State machine.** [status] changes only through [confirm], [reject], [expire], [complete] and
 * [cancelByProvider], which all funnel through one private function backed by
 * [BookingStatus.allowedTransitions] ([rate] attaches a rating but is not a status change). Each
 * returns the updated copy, or a [DomainError.InvalidInput] — never a silently allowed or ignored
 * invalid transition. (The
 * Customer's cancellation, `CancelledByCustomer`, is in the table but its transition function and
 * 3-hour rule belong to ticket #25.) Notifying the other party after a transition is deferred —
 * no notification system exists yet.
 *
 * Lifecycle and persistence are driven by the Scheduling use cases; see
 * [corp.khin.solutions.booqi.domain.usecase.SolicitarReservaUseCase] for creation.
 */
data class Booking(
    val id: String,
    val providerId: String,
    val serviceId: String,
    val customerId: String,
    val scheduledAt: LocalDateTime,
    val durationMinutesSnapshot: Int,
    val priceCentsSnapshot: Int,
    val requestedAt: Instant,
    val status: BookingStatus = BookingStatus.REQUESTED,
    val deliveryAddress: Address? = null,
    val customerNote: String? = null,
    val reason: Reason? = null,
    val rating: Rating? = null,
    val respondedAt: Instant? = null,
    val completedAt: Instant? = null,
) {
    /** The TimeSlot this Booking holds (while [BookingStatus.occupiesSlot]). */
    val timeSlot: TimeSlot get() = TimeSlot(providerId, scheduledAt.date, scheduledAt.time)

    /** The Provider must answer a [BookingStatus.REQUESTED] Booking before this moment. */
    val responseDeadline: Instant get() = requestedAt + RESPONSE_WINDOW

    /** True when an unanswered request is past its 24h window at [now] (boundary included). */
    fun isResponseOverdue(now: Instant): Boolean =
        status == BookingStatus.REQUESTED && now >= responseDeadline

    /**
     * True when this Booking holds a slot that overlaps `[start, start + durationMinutes)`.
     * Half-open: back-to-back appointments don't overlap. Compared on a neutral timeline (the
     * wall-clock values read as UTC) — it is only used to order two wall-clock values.
     */
    fun occupies(start: LocalDateTime, durationMinutes: Int): Boolean {
        if (!status.occupiesSlot) return false
        val mine = scheduledAt.toInstant(TimeZone.UTC)
        val other = start.toInstant(TimeZone.UTC)
        return mine < other + durationMinutes.minutes && other < mine + durationMinutesSnapshot.minutes
    }

    /** REQUESTED → CONFIRMED. Fails if not pending or if the 24h window already lapsed. */
    fun confirm(now: Instant): DomainResult<Booking> =
        answer(now, BookingStatus.CONFIRMED) { copy(respondedAt = now) }

    /**
     * REQUESTED → REJECTED with a predefined Provider reason ([code], plus an optional free-text
     * [note]). Typed as [ProviderReasonCode] so a Customer reason can't be used by mistake. Same
     * preconditions as [confirm].
     */
    fun reject(code: ProviderReasonCode, note: String?, now: Instant): DomainResult<Booking> =
        answer(now, BookingStatus.REJECTED) { copy(reason = Reason.of(code, note), respondedAt = now) }

    /**
     * REQUESTED → EXPIRED. Valid only once the window has lapsed ([isResponseOverdue]): expiring
     * a request that still has time left is rejected, so no trigger can cut the Provider short.
     */
    fun expire(now: Instant): DomainResult<Booking> = when {
        status == BookingStatus.REQUESTED && !isResponseOverdue(now) ->
            DomainError.InvalidInput("La solicitud aún está dentro de su plazo de 24 horas").asFailure()
        else -> moveTo(BookingStatus.EXPIRED) { this }
    }

    /** CONFIRMED → COMPLETED (manual, by the Provider). */
    fun complete(now: Instant): DomainResult<Booking> =
        moveTo(BookingStatus.COMPLETED) { copy(completedAt = now) }

    /** CONFIRMED → CANCELLED_BY_PROVIDER with a predefined Provider reason ([code] + optional [note]). */
    fun cancelByProvider(code: ProviderReasonCode, note: String?): DomainResult<Booking> =
        moveTo(BookingStatus.CANCELLED_BY_PROVIDER) { copy(reason = Reason.of(code, note)) }

    /**
     * Attaches the Customer's [rating]. Only a COMPLETED Booking, only once, stars within
     * [Rating.MIN_STARS]..[Rating.MAX_STARS]; a blank comment is stored as `null`. Not a status
     * transition — the Booking stays COMPLETED.
     */
    fun rate(rating: Rating): DomainResult<Booking> = when {
        status != BookingStatus.COMPLETED ->
            DomainError.InvalidInput("Solo se puede calificar una cita completada").asFailure()
        this.rating != null -> DomainError.InvalidInput("Esta cita ya fue calificada").asFailure()
        !rating.hasValidStars ->
            DomainError.InvalidInput(
                "La calificación debe ser de ${Rating.MIN_STARS} a ${Rating.MAX_STARS} estrellas",
            ).asFailure()
        else -> copy(rating = rating.copy(comment = rating.comment?.trim()?.takeIf { it.isNotEmpty() }))
            .asSuccess()
    }

    // A Provider's answer (accept/reject) is only meaningful inside the response window.
    private fun answer(now: Instant, target: BookingStatus, change: Booking.() -> Booking): DomainResult<Booking> =
        if (isResponseOverdue(now)) {
            DomainError.InvalidInput("La solicitud venció: pasaron más de 24 horas sin respuesta").asFailure()
        } else {
            moveTo(target, change)
        }

    // The one place a status changes: checks the table, then applies [change] to the new state.
    private fun moveTo(target: BookingStatus, change: Booking.() -> Booking): DomainResult<Booking> =
        if (status.canTransitionTo(target)) {
            copy(status = target).change().asSuccess()
        } else {
            DomainError.InvalidInput("Una reserva en estado $status no puede pasar a $target").asFailure()
        }

    companion object {
        /** How long the Provider has to answer a request before it expires. */
        val RESPONSE_WINDOW: Duration = 24.hours

        /** Builds the initial REQUESTED Booking for [draft] under the storage-assigned [id]. */
        fun requested(id: String, draft: BookingDraft): Booking = Booking(
            id = id,
            providerId = draft.providerId,
            serviceId = draft.serviceId,
            customerId = draft.customerId,
            scheduledAt = draft.scheduledAt,
            durationMinutesSnapshot = draft.durationMinutesSnapshot,
            priceCentsSnapshot = draft.priceCentsSnapshot,
            requestedAt = draft.requestedAt,
            deliveryAddress = draft.deliveryAddress,
            customerNote = draft.customerNote,
        )
    }
}

/**
 * Everything needed to create a [Booking] — what `SolicitarReserva` has assembled (snapshots
 * already copied) before storage assigns the id. Groups the fields into one value (detekt
 * `LongParameterList`) and keeps "a Booking always starts REQUESTED" in [Booking.requested].
 */
data class BookingDraft(
    val providerId: String,
    val serviceId: String,
    val customerId: String,
    val scheduledAt: LocalDateTime,
    val durationMinutesSnapshot: Int,
    val priceCentsSnapshot: Int,
    val requestedAt: Instant,
    val deliveryAddress: Address? = null,
    val customerNote: String? = null,
)
