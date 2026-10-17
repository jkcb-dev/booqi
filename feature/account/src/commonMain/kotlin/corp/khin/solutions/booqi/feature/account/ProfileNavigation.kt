package corp.khin.solutions.booqi.feature.account

/**
 * Where the profile screen can lead, as one parameter object so the screen keeps a short signature.
 * Navigation is exposed as callbacks so Architect's wiring maps them to `Navigator` calls without
 * this module importing another feature. [onProviderMode] fires only after the access guard passed;
 * [onSignInRequired] / [onSignUpRequested] / [onVerificationRequired] push the sign-in flow or the
 * verification notice over this screen, so finishing them returns here; [onSessionEnded] fires after
 * signing out or deleting the account, when the app goes back to browsing as a visitor.
 */
class ProfileNavigation(
    val onProviderMode: () -> Unit,
    val onSignInRequired: () -> Unit,
    val onSignUpRequested: () -> Unit,
    val onVerificationRequired: () -> Unit,
    val onSessionEnded: () -> Unit,
)
