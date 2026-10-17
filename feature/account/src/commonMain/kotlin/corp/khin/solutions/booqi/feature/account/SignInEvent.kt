package corp.khin.solutions.booqi.feature.account

/** One-shot effects of the sign-in form — never folded into [SignInUiState] (they would replay). */
sealed interface SignInEvent {
    /** The session started. [needsEmailVerification] is true when the account's email is unverified. */
    data class Authenticated(val needsEmailVerification: Boolean) : SignInEvent
    data object SwitchToSignUp : SignInEvent
    data object Close : SignInEvent
}
