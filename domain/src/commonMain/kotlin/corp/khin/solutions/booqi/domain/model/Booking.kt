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
 * [cancelByProvider] and [cancelByCustomer], which all funnel through one private function backed by
 * [BookingStatus.allowedTransitions] ([rate] attaches a rating but is not a status change). Each
 * returns the updated copy, or a [DomainError.InvalidInput] — never a silently allowed or ignored
 * invalid transition. The Customer's cancellation also carries the **3-hour rule** (#25), see
 * [cancelByCustomer]. Notifying the other party after a transition is deferred — no notification
 * system exists yet.
 *
 * **Customer name snapshot (#25).** [customerNameSnapshot] is the Customer's display name copied
 * when the request was made (`SolicitarReserva`), so the Provider's inbox can show who booked
 * without a live lookup that drifts when the Customer renames or deletes their account (the
 * snapshot is erased on account deletion, like the address and note). `null` for Bookings created
 * before the snapshot existed — screens fall back to the live `User` name until they switch to it.
 *
 * Lifecycle and persistence are driven by the Scheduling use cases; see
 * [corp.khin.solutions.booqi.domain.usecase.SolicitarReservaUseCase] for creation.
 */
// One function per state-machine transition plus its two private helpers: splitting the aggregate's
// behaviour across files would hide the single path every status change must take.
@Suppress("TooManyFunctions")
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
    val customerNameSnapshot: String? = null,
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
     * CONFIRMED → CANCELLED_BY_CUSTOMER with a predefined Customer reason ([code] + optional
     * [note]), allowed **up to 3 hours before** the appointment ([CUSTOMER_CANCELLATION_WINDOW]):
     * `now <= start - 3h` is accepted — exactly three hours before still counts ("hasta 3 horas
     * antes"), one instant later is refused. [scheduledAt] is the Provider's wall-clock start, read
     * in [timeZone] (the same interpretation the UI uses for "has it ended", there is no timezone
     * feature yet). A Booking that isn't CONFIRMED gets the usual invalid-transition error, whatever
     * the time; a CONFIRMED one that is too late gets [TOO_LATE_TO_CANCEL_MESSAGE].
     */
    fun cancelByCustomer(
        code: CustomerReasonCode,
        note: String?,
        now: Instant,
        timeZone: TimeZone,
    ): DomainResult<Booking> =
        if (status.canTransitionTo(BookingStatus.CANCELLED_BY_CUSTOMER) &&
            now > scheduledAt.toInstant(timeZone) - CUSTOMER_CANCELLATION_WINDOW
        ) {
            DomainError.InvalidInput(TOO_LATE_TO_CANCEL_MESSAGE).asFailure()
        } else {
            moveTo(BookingStatus.CANCELLED_BY_CUSTOMER) { copy(reason = Reason.of(code, note)) }
        }

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

        /** How long before the appointment the Customer may still cancel a confirmed Booking. */
        val CUSTOMER_CANCELLATION_WINDOW: Duration = 3.hours

        const val TOO_LATE_TO_CANCEL_MESSAGE =
            "Ya no se puede cancelar: la cancelación es hasta 3 horas antes de la cita"

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
            customerNameSnapshot = draft.customerNameSnapshot,
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
    val customerNameSnapshot: String? = null,
)
