package corp.khin.solutions.booqi.feature.account

/** User intents on the profile screen. The Composable only ever calls [ProfileViewModel.onAction]. */
sealed interface ProfileAction {
    /** Sent on every entry: reloads who is signed in (the account may have changed since). */
    data object Start : ProfileAction

    data object EditClicked : ProfileAction
    data object CancelEditClicked : ProfileAction
    data class NameChanged(val value: String) : ProfileAction
    data class PhotoUrlChanged(val value: String) : ProfileAction
    data object SaveClicked : ProfileAction
    data object SignOutClicked : ProfileAction

    /** "Convertirme en Proveedor" / "Ir a mi panel de Proveedor": passes the access guard first. */
    data object ProviderEntryClicked : ProfileAction

    data object VerifyEmailClicked : ProfileAction
    data object SignInClicked : ProfileAction
    data object SignUpClicked : ProfileAction
}
