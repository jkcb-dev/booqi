package corp.khin.solutions.booqi.feature.booking

import corp.khin.solutions.booqi.domain.model.Address

/** User intents on the address picker. The Composable only ever calls [AddressViewModel.onAction]. */
sealed interface AddressAction {
    /** Sent on every entry: resets the picker and reads the saved address again. */
    data object Start : AddressAction

    data object Retry : AddressAction
    data class QueryChanged(val value: String) : AddressAction

    /** Candidate from text search *or* from the map pin (platform reverse-geocode, #24). */
    data class ResultSelected(val address: Address) : AddressAction

    /** "Guardar dirección": adds the selected address, or updates the saved one. */
    data object SaveClicked : AddressAction
    data object BackClicked : AddressAction
}
