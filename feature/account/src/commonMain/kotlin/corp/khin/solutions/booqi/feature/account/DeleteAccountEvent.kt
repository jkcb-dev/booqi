package corp.khin.solutions.booqi.feature.account

/** One-shot effects of the account-deletion flow — never folded into [DeleteAccountUiState]. */
sealed interface DeleteAccountEvent {
    /** The account was anonymized and the session ended: the app goes back to browsing as a visitor. */
    data object AccountDeleted : DeleteAccountEvent
}
