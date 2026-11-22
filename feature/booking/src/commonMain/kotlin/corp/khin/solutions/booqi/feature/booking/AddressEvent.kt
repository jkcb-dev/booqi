package corp.khin.solutions.booqi.feature.booking

/** One-shot effects of the address picker — never folded into [AddressUiState] (they would replay). */
sealed interface AddressEvent {
    /** Leave the picker: the address was saved, or the Customer backed out. */
    data object Finished : AddressEvent
}
