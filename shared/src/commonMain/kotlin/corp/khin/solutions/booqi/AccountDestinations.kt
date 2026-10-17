package corp.khin.solutions.booqi

import androidx.compose.runtime.Composable
import corp.khin.solutions.booqi.core.navigation.Destination
import corp.khin.solutions.booqi.core.navigation.Navigator
import corp.khin.solutions.booqi.feature.account.AuthScreen
import corp.khin.solutions.booqi.feature.account.ProfileNavigation
import corp.khin.solutions.booqi.feature.account.ProfileScreen
import corp.khin.solutions.booqi.feature.account.VerifyEmailScreen

/**
 * `feature:account` — Identity. [Destination.Auth] and [Destination.VerifyEmail] are always pushed
 * over the screen that needed an account ("Reservar", "Convertirme en Proveedor", the profile), so
 * finishing them is a plain back: the visitor returns to where they were. A new email+password
 * account continues from sign-up to the verification notice, which then also pops back.
 */
@Composable
internal fun AccountDestination(destination: Destination, navigator: Navigator) {
    when (destination) {
        is Destination.Profile -> ProfileScreen(
            navigation = ProfileNavigation(
                onProviderMode = { navigator.navigateToRoot(Destination.ProviderProfileSetup) },
                onSignInRequired = { navigator.navigateTo(Destination.Auth()) },
                onSignUpRequested = { navigator.navigateTo(Destination.Auth(startOnSignUp = true)) },
                onVerificationRequired = { navigator.navigateTo(Destination.VerifyEmail) },
                // Signed out or deleted: the app goes back to browsing as a visitor.
                onSessionEnded = { navigator.navigateToRoot(Destination.Browse) },
            ),
        )
        is Destination.Auth -> AuthScreen(
            startOnSignUp = destination.startOnSignUp,
            onAuthenticated = { needsEmailVerification ->
                navigator.backOr(Destination.Profile)
                if (needsEmailVerification) navigator.navigateTo(Destination.VerifyEmail)
            },
            onClose = { navigator.backOr(Destination.Browse) },
        )
        is Destination.VerifyEmail -> VerifyEmailScreen(
            onVerified = { navigator.backOr(Destination.Profile) },
            onClose = { navigator.backOr(Destination.Profile) },
        )
        else -> Unit
    }
}
