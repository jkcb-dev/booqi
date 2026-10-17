package corp.khin.solutions.booqi.feature.account

import corp.khin.solutions.booqi.domain.model.SocialProvider

/** User intents on the sign-up form. The Composable only ever calls [SignUpViewModel.onAction]. */
sealed interface SignUpAction {
    /** Sent on every entry: the ViewModel outlives a visit, so the form starts clean each time. */
    data object Start : SignUpAction

    data class NameChanged(val value: String) : SignUpAction
    data class EmailChanged(val value: String) : SignUpAction
    data class PasswordChanged(val value: String) : SignUpAction
    data object SubmitClicked : SignUpAction
    data class SocialClicked(val provider: SocialProvider) : SignUpAction
    data object SwitchToSignInClicked : SignUpAction
    data object CloseClicked : SignUpAction
}
