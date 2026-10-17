package corp.khin.solutions.booqi.feature.account

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

/**
 * Iniciar sesión / crear cuenta, for `Destination.Auth(startOnSignUp)`
 * (`corp.khin.solutions.booqi.core.navigation.Destination`): one destination hosting both forms, so
 * switching between them never grows the back stack and finishing always pops exactly one entry —
 * back to wherever the visitor was when an account became necessary.
 *
 * [onAuthenticated] fires once the session started; [needsEmailVerification] is true for a new
 * email+password account, which the wiring sends on to the verification notice. [onClose] is
 * "Volver" without signing in.
 */
@Composable
fun AuthScreen(
    startOnSignUp: Boolean,
    onAuthenticated: (needsEmailVerification: Boolean) -> Unit,
    onClose: () -> Unit,
) {
    var showSignUp by remember(startOnSignUp) { mutableStateOf(startOnSignUp) }
    if (showSignUp) {
        SignUpScreen(
            onAuthenticated = onAuthenticated,
            onSwitchToSignIn = { showSignUp = false },
            onClose = onClose,
        )
    } else {
        SignInScreen(
            onAuthenticated = onAuthenticated,
            onSwitchToSignUp = { showSignUp = true },
            onClose = onClose,
        )
    }
}
