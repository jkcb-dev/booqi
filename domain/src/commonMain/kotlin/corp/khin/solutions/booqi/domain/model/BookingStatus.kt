package corp.khin.solutions.booqi.domain.model

/**
 * The Booking lifecycle (docs/DOMAIN.md, docs/domain/provider-flow.md § Grupo 4). The names are the
 * ubiquitous-language states (`REQUESTED` is the docs' "Requested", ...), stored as those exact
 * strings by the data layer.
 *
 * ```
 * REQUESTED ──► CONFIRMED ──► COMPLETED
 *     │              ├──────► CANCELLED_BY_PROVIDER
 *     │              └──────► CANCELLED_BY_CUSTOMER
 *     ├──► REJECTED
 *     └──► EXPIRED   (24h without a response)
 * ```
 *
 * **This table is the single source of truth for which transitions exist**: every state change in
 * the app goes through [Booking]'s transition functions, which consult [canTransitionTo]; no use
 * case decides on its own. Anything not listed is invalid — there are no shortcuts.
 */
enum class BookingStatus {
    REQUESTED,
    CONFIRMED,
    COMPLETED,
    REJECTED,
    EXPIRED,
    CANCELLED_BY_PROVIDER,
    CANCELLED_BY_CUSTOMER,
    ;

    /** The only states a Booking in this state may move to; empty for terminal states. */
    val allowedTransitions: Set<BookingStatus>
        get() = when (this) {
            REQUESTED -> setOf(CONFIRMED, REJECTED, EXPIRED)
            CONFIRMED -> setOf(COMPLETED, CANCELLED_BY_PROVIDER, CANCELLED_BY_CUSTOMER)
            COMPLETED, REJECTED, EXPIRED, CANCELLED_BY_PROVIDER, CANCELLED_BY_CUSTOMER -> emptySet()
        }

    /** True when a Booking in this state may move to [target]. */
    fun canTransitionTo(target: BookingStatus): Boolean = target in allowedTransitions

    /** True when no further transition is possible. */
    val isTerminal: Boolean get() = allowedTransitions.isEmpty()

    /**
     * True while the Booking is still open — `REQUESTED` or `CONFIRMED`, i.e. not [isTerminal].
     * An account with such a Booking (as Customer or as Provider) cannot be deleted.
     */
    val isActive: Boolean get() = !isTerminal

    /**
     * True while a Booking in this state holds its TimeSlot: pending and confirmed ones do;
     * rejected, expired and cancelled ones free it again ("el TimeSlot vuelve a estar
     * disponible"). A completed one is in the past and no longer competes for a future slot.
     */
    val occupiesSlot: Boolean get() = this == REQUESTED || this == CONFIRMED
}
