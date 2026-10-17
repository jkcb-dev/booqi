package corp.khin.solutions.booqi.feature.account

/** User intents of the account-deletion flow. The Composable only calls [DeleteAccountViewModel.onAction]. */
sealed interface DeleteAccountAction {
    /** Sent on every entry: closes anything a previous visit left open. */
    data object Start : DeleteAccountAction

    /** "Eliminar cuenta" — opens the confirmation. */
    data object DeleteClicked : DeleteAccountAction

    /** "Sí, eliminar mi cuenta". */
    data object ConfirmClicked : DeleteAccountAction

    /** Closes the confirmation or the list of blocking Bookings. */
    data object DismissClicked : DeleteAccountAction
}
