package corp.khin.solutions.booqi.feature.provider

import corp.khin.solutions.booqi.domain.model.ProviderReasonCode

/** User intents on the booking inbox. The Composable only ever calls
 * [BookingInboxViewModel.onAction] with one of these. */
sealed interface BookingInboxAction {
    /** Sent on every entry: resets to the "Solicitudes" tab, drops any open detail and reloads. */
    data object Start : BookingInboxAction

    /** Reloads both lists (the "Reintentar" button). */
    data object Refresh : BookingInboxAction

    /** Periodic nudge that re-reads the clock so "Completar" enables itself at the end time. */
    data object Tick : BookingInboxAction

    data class SelectTab(val tab: BookingInboxTab) : BookingInboxAction

    /** Opens a row's detail; [reasonIntent] opens it with the reject/cancel picker already showing
     * (the "Rechazar" button on a P8 row). */
    data class OpenBooking(val bookingId: String, val reasonIntent: ReasonIntent? = null) : BookingInboxAction

    /** "Volver": closes the open reason form, then the detail, then leaves the screen. */
    data object BackClicked : BookingInboxAction
    data object DismissNotice : BookingInboxAction

    /** "Aceptar" on a request, from its P8 row or its P9 detail. */
    data class Accept(val bookingId: String) : BookingInboxAction

    /** "Rechazar" (P9) or "Cancelar cita" (P10): opens the reason picker. */
    data class OpenReasonForm(val intent: ReasonIntent) : BookingInboxAction
    data class ReasonSelected(val code: ProviderReasonCode) : BookingInboxAction
    data class ReasonNoteChanged(val value: String) : BookingInboxAction

    /** Confirms the open reason picker: rejects or cancels, depending on its intent. */
    data object ConfirmReason : BookingInboxAction

    /** "Marcar como completada" (P10). */
    data object Complete : BookingInboxAction
}
