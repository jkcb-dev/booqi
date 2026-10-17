package corp.khin.solutions.booqi.feature.account

/** One-shot effects of the sign-up form — never folded into [SignUpUiState] (they would replay). */
sealed interface SignUpEvent {
    /**
     * The account exists and its session started. [needsEmailVerification] is true for an
     * email+password account (a verification email was sent); social accounts arrive verified.
     */
    data class Authenticated(val needsEmailVerification: Boolean) : SignUpEvent
    data object SwitchToSignIn : SignUpEvent
    data object Close : SignUpEvent
}
