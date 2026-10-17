package corp.khin.solutions.booqi.feature.booking

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import corp.khin.solutions.booqi.core.designsystem.theme.BooqiSpacing
import corp.khin.solutions.booqi.core.designsystem.theme.LocalBooqiExtendedColors

/**
 * C9 — the confirmed appointment and its "Cancelar cita". The cancellation window is 3 hours
 * before the start (docs/domain/customer-flow.md § Grupo 3): inside it the button is disabled and
 * says why, instead of disappearing without explanation. Once tapped the shared `ReasonPicker`
 * (the CUSTOMER reasons) takes the place of the button, see [CancelSection].
 */
@Composable
internal fun ConfirmedSection(state: BookingDetailUiState, onAction: (BookingDetailAction) -> Unit) {
    val form = state.cancelForm
    Column(verticalArrangement = Arrangement.spacedBy(BooqiSpacing.sm)) {
        if (form != null) {
            CancelSection(form = form, isCancelling = state.isCancelling, onAction = onAction)
        } else {
            OutlinedButton(
                onClick = { onAction(BookingDetailAction.OpenCancelForm) },
                enabled = state.canCancel,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Cancelar cita") }
            if (!state.canCancel) {
                Text(
                    text = "Ya no se puede cancelar: la cancelación es hasta 3 horas antes de la cita.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = LocalBooqiExtendedColors.current.ink2,
                )
            }
        }
    }
}
