package corp.khin.solutions.booqi.feature.account

/** One-shot effects of the profile screen — never folded into [ProfileUiState] (they would replay). */
sealed interface ProfileEvent {
    /** The access guard passed: the User may open Provider mode. */
    data object NavigateToProviderMode : ProfileEvent

    /** The guard found no session: ask the visitor to sign in (and come back here afterwards). */
    data object NavigateToSignIn : ProfileEvent

    data object NavigateToSignUp : ProfileEvent

    /** The guard found an unverified email: show the verification notice. */
    data object NavigateToVerifyEmail : ProfileEvent

    /** The session ended: the app goes back to browsing as a visitor. */
    data object SignedOut : ProfileEvent
}
