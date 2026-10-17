package corp.khin.solutions.booqi.feature.account

/** One-shot effects of the verify-email notice — never folded into [VerifyEmailUiState]. */
sealed interface VerifyEmailEvent {
    /** The account is verified: go back to what the User was doing. */
    data object Verified : VerifyEmailEvent

    /** Leave without verifying. */
    data object Close : VerifyEmailEvent
}
