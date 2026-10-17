package corp.khin.solutions.booqi.feature.booking

import corp.khin.solutions.booqi.domain.model.TimeSlot

/** User intents on the request flow. The Composable only ever calls [BookingFlowViewModel.onAction]. */
sealed interface BookingFlowAction {
    /**
     * Sent on every entry for [serviceId]. Resets the flow and loads that Service afresh — except
     * when the Customer left to sign in or to add an address and is coming back to the same
     * Service: then the chosen slot, note and step are kept (identity-flow.md "vuelve a la reserva
     * que estaba haciendo, con el TimeSlot elegido") and only the data is refreshed.
     */
    data class Start(val serviceId: String) : BookingFlowAction

    data object Retry : BookingFlowAction
    data object PreviousDay : BookingFlowAction
    data object NextDay : BookingFlowAction
    data class SlotSelected(val slot: TimeSlot) : BookingFlowAction

    /** "Continuar" on C5: moves to the confirmation (C7). */
    data object ContinueClicked : BookingFlowAction

    /** Back: from C7 to C5 (selection kept); from C5 it leaves the flow. */
    data object BackClicked : BookingFlowAction
    data class AtHomeChanged(val atHome: Boolean) : BookingFlowAction
    data class NoteChanged(val value: String) : BookingFlowAction

    /** "Enviar solicitud". */
    data object Submit : BookingFlowAction

    /** C7's "Agregar dirección" (address screen is #23, outside this module). */
    data object AddAddressClicked : BookingFlowAction
    data object RetryAddress : BookingFlowAction

    /** The sign-in / verify-email button of the access notice. */
    data object SignInClicked : BookingFlowAction
}
