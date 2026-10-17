package corp.khin.solutions.booqi.feature.account

import corp.khin.solutions.booqi.domain.model.SocialProvider

/** User intents on the sign-in form. The Composable only ever calls [SignInViewModel.onAction]. */
sealed interface SignInAction {
    /** Sent on every entry: the ViewModel outlives a visit, so the form starts clean each time. */
    data object Start : SignInAction

    data class EmailChanged(val value: String) : SignInAction
    data class PasswordChanged(val value: String) : SignInAction
    data object SignInClicked : SignInAction
    data class SocialClicked(val provider: SocialProvider) : SignInAction
    data object SwitchToSignUpClicked : SignInAction
    data object CloseClicked : SignInAction
}
