package corp.khin.solutions.booqi.feature.booking

import corp.khin.solutions.booqi.domain.model.CustomerReasonCode

/** User intents on the Booking detail. The Composable only ever calls [BookingDetailViewModel.onAction]. */
sealed interface BookingDetailAction {
    /** Sent on every entry for [bookingId]: resets the screen and loads that Booking afresh. */
    data class Start(val bookingId: String) : BookingDetailAction

    data object Retry : BookingDetailAction

    /** Keeps the countdowns and the 3h rule current while the screen stays open. */
    data object Tick : BookingDetailAction

    /** "Cancelar cita": opens the reason picker (refused inside the 3h window). */
    data object OpenCancelForm : BookingDetailAction
    data object CloseCancelForm : BookingDetailAction
    data class ReasonSelected(val code: CustomerReasonCode) : BookingDetailAction
    data class ReasonNoteChanged(val value: String) : BookingDetailAction
    data object ConfirmCancel : BookingDetailAction
    data object DismissNotice : BookingDetailAction

    /** C12: "Calificar". */
    data object RateClicked : BookingDetailAction

    /** C10 / C10b: "Buscar otro horario" — back into the request flow for the same Service. */
    data object RebookClicked : BookingDetailAction

    /** C10 / C10b: "Volver al inicio". */
    data object GoHomeClicked : BookingDetailAction
    data object BackClicked : BookingDetailAction
}
