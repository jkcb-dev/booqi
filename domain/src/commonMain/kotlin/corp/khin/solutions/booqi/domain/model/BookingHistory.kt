package corp.khin.solutions.booqi.domain.model

/**
 * A Customer's Bookings ("Mis reservas", docs/domain/customer-flow.md § Grupo 3) separated by
 * [BookingStatus]: one [BookingHistoryGroup] per status that has at least one Booking, in
 * [STATUS_ORDER]. Read model, not an aggregate.
 */
data class BookingHistory(val groups: List<BookingHistoryGroup>) {
    val isEmpty: Boolean get() = groups.isEmpty()

    /** Every Booking, flattened in group order. */
    val bookings: List<Booking> get() = groups.flatMap { it.bookings }

    /** The group of [status], or `null` when the Customer has none in that state. */
    fun group(status: BookingStatus): BookingHistoryGroup? = groups.firstOrNull { it.status == status }

    companion object {
        /**
         * Pending first, then confirmed (what the Customer may still act on), then the closed
         * ones: completed, the Customer's own cancellations, the Provider's cancellations,
         * rejected, expired.
         */
        val STATUS_ORDER: List<BookingStatus> = listOf(
            BookingStatus.REQUESTED,
            BookingStatus.CONFIRMED,
            BookingStatus.COMPLETED,
            BookingStatus.CANCELLED_BY_CUSTOMER,
            BookingStatus.CANCELLED_BY_PROVIDER,
            BookingStatus.REJECTED,
            BookingStatus.EXPIRED,
        )

        /**
         * Groups [bookings] by status in [STATUS_ORDER]. Open statuses ([BookingStatus.isActive])
         * list the soonest appointment first; closed ones the most recent first; ties by id.
         */
        fun of(bookings: List<Booking>): BookingHistory = BookingHistory(
            STATUS_ORDER.mapNotNull { status ->
                val ofStatus = bookings.filter { it.status == status }
                if (ofStatus.isEmpty()) {
                    null
                } else {
                    val order = if (status.isActive) {
                        compareBy<Booking>({ it.scheduledAt }, { it.id })
                    } else {
                        compareByDescending<Booking> { it.scheduledAt }.thenBy { it.id }
                    }
                    BookingHistoryGroup(status, ofStatus.sortedWith(order))
                }
            },
        )
    }
}

/** The Customer's Bookings that are in [status] (never empty when it comes from [BookingHistory.of]). */
data class BookingHistoryGroup(val status: BookingStatus, val bookings: List<Booking>)
