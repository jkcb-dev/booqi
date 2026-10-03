package corp.khin.solutions.booqi.feature.browse

/** One-shot effects of C3 — never folded into [ServiceDetailUiState] (they would replay). */
sealed interface ServiceDetailEvent {
    data class NavigateToProvider(val providerId: String) : ServiceDetailEvent
    data class NavigateToBooking(val serviceId: String, val providerId: String) : ServiceDetailEvent
    data object NavigateBack : ServiceDetailEvent
}
