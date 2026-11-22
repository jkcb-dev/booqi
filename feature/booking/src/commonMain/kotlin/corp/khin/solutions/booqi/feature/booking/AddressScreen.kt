package corp.khin.solutions.booqi.feature.booking

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import org.koin.compose.viewmodel.koinViewModel

/**
 * C6 — the Customer's address picker, for `Destination.AddressSelection`: search, pick, save.
 * [onFinished] is called when the address was saved or the Customer backs out; wiring maps it to a
 * pop back to whoever opened it (the booking flow, which then re-reads the saved address, or the
 * profile). Reloads on every entry.
 */
@Composable
fun AddressScreen(
    onFinished: () -> Unit,
    viewModel: AddressViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val currentOnFinished by rememberUpdatedState(onFinished)

    // Reset/reload on every entry: the ViewModel instance is reused across visits (see
    // AddressViewModel), so add -> back -> add again never shows the previous query or selection.
    LaunchedEffect(Unit) { viewModel.onAction(AddressAction.Start) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                AddressEvent.Finished -> currentOnFinished()
            }
        }
    }

    AddressContent(state = state, onAction = viewModel::onAction)
}
