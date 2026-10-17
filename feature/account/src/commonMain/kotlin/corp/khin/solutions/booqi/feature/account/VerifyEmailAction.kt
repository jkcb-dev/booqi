package corp.khin.solutions.booqi.feature.account

/** User intents on the verify-email notice. The Composable only calls [VerifyEmailViewModel.onAction]. */
sealed interface VerifyEmailAction {
    /** Sent on every entry: reloads who is signed in and drops a previous visit's messages. */
    data object Start : VerifyEmailAction

    /** "Ya verifiqué mi correo". */
    data object ConfirmClicked : VerifyEmailAction

    /** "Reenviar correo de verificación". */
    data object ResendClicked : VerifyEmailAction

    /** "Más tarde" — leaves without verifying. */
    data object LaterClicked : VerifyEmailAction
}
