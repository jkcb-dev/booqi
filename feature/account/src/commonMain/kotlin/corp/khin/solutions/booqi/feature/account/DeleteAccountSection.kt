package corp.khin.solutions.booqi.feature.account

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import org.koin.compose.viewmodel.koinViewModel

/**
 * The "Eliminar cuenta" button of the profile and the dialogs of its flow (confirmation, then the
 * list of blocking Bookings if there are any). [onAccountDeleted] fires once the account is gone
 * and the session ended. Resets on every entry.
 */
@Composable
internal fun DeleteAccountSection(
    onAccountDeleted: () -> Unit,
    viewModel: DeleteAccountViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val currentOnAccountDeleted by rememberUpdatedState(onAccountDeleted)

    // A closed flow on every entry: the ViewModel instance is reused across visits.
    LaunchedEffect(Unit) { viewModel.onAction(DeleteAccountAction.Start) }

    // Effects are collected once, separately from state — see DeleteAccountEvent for why.
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                DeleteAccountEvent.AccountDeleted -> currentOnAccountDeleted()
            }
        }
    }

    OutlinedButton(
        onClick = { viewModel.onAction(DeleteAccountAction.DeleteClicked) },
        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
        modifier = Modifier.fillMaxWidth(),
    ) { Text("Eliminar cuenta") }

    DeleteAccountDialogs(state = state, onAction = viewModel::onAction)
}
