package corp.khin.solutions.booqi.feature.account

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import org.koin.compose.viewmodel.koinViewModel

/** The sign-up form, hosted by [AuthScreen]; navigation is exposed as callbacks, like [SignInScreen]. */
@Composable
internal fun SignUpScreen(
    onAuthenticated: (needsEmailVerification: Boolean) -> Unit,
    onSwitchToSignIn: () -> Unit,
    onClose: () -> Unit,
    viewModel: SignUpViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val currentOnAuthenticated by rememberUpdatedState(onAuthenticated)
    val currentOnSwitchToSignIn by rememberUpdatedState(onSwitchToSignIn)
    val currentOnClose by rememberUpdatedState(onClose)

    // A clean form on every entry: the ViewModel instance is reused across visits.
    LaunchedEffect(Unit) { viewModel.onAction(SignUpAction.Start) }

    // Effects are collected once, separately from state — see SignUpEvent for why.
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is SignUpEvent.Authenticated -> currentOnAuthenticated(event.needsEmailVerification)
                SignUpEvent.SwitchToSignIn -> currentOnSwitchToSignIn()
                SignUpEvent.Close -> currentOnClose()
            }
        }
    }

    SignUpContent(state = state, onAction = viewModel::onAction)
}
