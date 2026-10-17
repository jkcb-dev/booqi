package corp.khin.solutions.booqi.feature.account

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import org.koin.compose.viewmodel.koinViewModel

/**
 * The "verificá tu correo" notice, for `Destination.VerifyEmail`
 * (`corp.khin.solutions.booqi.core.navigation.Destination`). [onVerified] and [onClose] both lead
 * back to what the User was doing; they are separate so the wiring can tell them apart.
 */
@Composable
fun VerifyEmailScreen(
    onVerified: () -> Unit,
    onClose: () -> Unit,
    viewModel: VerifyEmailViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val currentOnVerified by rememberUpdatedState(onVerified)
    val currentOnClose by rememberUpdatedState(onClose)

    // Reload who is signed in on every entry: the ViewModel instance is reused across visits.
    LaunchedEffect(Unit) { viewModel.onAction(VerifyEmailAction.Start) }

    // Effects are collected once, separately from state — see VerifyEmailEvent for why.
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                VerifyEmailEvent.Verified -> currentOnVerified()
                VerifyEmailEvent.Close -> currentOnClose()
            }
        }
    }

    VerifyEmailContent(state = state, onAction = viewModel::onAction)
}
