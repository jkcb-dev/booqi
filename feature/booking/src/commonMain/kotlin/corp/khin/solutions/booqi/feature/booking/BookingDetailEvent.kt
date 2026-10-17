package corp.khin.solutions.booqi.feature.booking

/** One-shot effects of the Booking detail — never folded into [BookingDetailUiState] (they would replay). */
sealed interface BookingDetailEvent {
    data class Rebook(val serviceId: String) : BookingDetailEvent
    data class Rate(val bookingId: String) : BookingDetailEvent
    data object GoHome : BookingDetailEvent
    data object Finished : BookingDetailEvent
}
