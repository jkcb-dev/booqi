package corp.khin.solutions.booqi.feature.account

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import org.koin.compose.viewmodel.koinViewModel

/**
 * The sign-in form, hosted by [AuthScreen]. Navigation is exposed as callbacks so Architect's
 * wiring maps them to `Navigator` calls without this module importing another feature.
 */
@Composable
internal fun SignInScreen(
    onAuthenticated: (needsEmailVerification: Boolean) -> Unit,
    onSwitchToSignUp: () -> Unit,
    onClose: () -> Unit,
    viewModel: SignInViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val currentOnAuthenticated by rememberUpdatedState(onAuthenticated)
    val currentOnSwitchToSignUp by rememberUpdatedState(onSwitchToSignUp)
    val currentOnClose by rememberUpdatedState(onClose)

    // A clean form on every entry: the ViewModel instance is reused across visits.
    LaunchedEffect(Unit) { viewModel.onAction(SignInAction.Start) }

    // Effects are collected once, separately from state — see SignInEvent for why.
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is SignInEvent.Authenticated -> currentOnAuthenticated(event.needsEmailVerification)
                SignInEvent.SwitchToSignUp -> currentOnSwitchToSignUp()
                SignInEvent.Close -> currentOnClose()
            }
        }
    }

    SignInContent(state = state, onAction = viewModel::onAction)
}
