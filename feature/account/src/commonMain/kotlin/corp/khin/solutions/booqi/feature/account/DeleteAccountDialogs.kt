package corp.khin.solutions.booqi.feature.account

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import corp.khin.solutions.booqi.core.designsystem.theme.BooqiSpacing
import corp.khin.solutions.booqi.domain.model.ActiveBookings
import corp.khin.solutions.booqi.domain.model.Booking

/** Whichever dialog the deletion flow is in: the blocking Bookings if refused, else the confirmation. */
@Composable
internal fun DeleteAccountDialogs(state: DeleteAccountUiState, onAction: (DeleteAccountAction) -> Unit) {
    val blocked = state.blocked
    when {
        blocked != null -> BlockedDialog(blocked, state.serviceTitles) { onAction(DeleteAccountAction.DismissClicked) }
        state.isConfirming -> ConfirmDialog(state, onAction)
    }
}

@Composable
private fun ConfirmDialog(state: DeleteAccountUiState, onAction: (DeleteAccountAction) -> Unit) {
    AlertDialog(
        onDismissRequest = { onAction(DeleteAccountAction.DismissClicked) },
        title = { Text("¿Eliminar tu cuenta?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(BooqiSpacing.xs)) {
                Text(
                    "Se borrarán tu nombre, tu foto y tus datos personales, y se cerrará tu sesión. " +
                        "Tus reservas completadas y tus reseñas se conservan como \"Usuario eliminado\".",
                )
                state.error?.let { ErrorText(it.describe()) }
            }
        },
        confirmButton = {
            TextButton(onClick = { onAction(DeleteAccountAction.ConfirmClicked) }, enabled = !state.isDeleting) {
                Text(
                    text = if (state.isDeleting) "Eliminando..." else "Sí, eliminar mi cuenta",
                    color = MaterialTheme.colorScheme.error,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = { onAction(DeleteAccountAction.DismissClicked) }, enabled = !state.isDeleting) {
                Text("Cancelar")
            }
        },
    )
}

/** Escenario: "El Usuario intenta eliminar su cuenta con reservas activas" — which ones to cancel or complete. */
@Composable
private fun BlockedDialog(active: ActiveBookings, serviceTitles: Map<String, String>, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("No podés eliminar tu cuenta todavía") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(BooqiSpacing.sm)) {
                Text("Cancelá o completá estas reservas primero:")
                BlockingGroup("Como Cliente", active.asCustomer, serviceTitles)
                BlockingGroup("Como Proveedor", active.asProvider, serviceTitles)
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Entendido") } },
    )
}

@Composable
private fun BlockingGroup(title: String, bookings: List<Booking>, serviceTitles: Map<String, String>) {
    if (bookings.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(BooqiSpacing.xxs)) {
        Text(title, style = MaterialTheme.typography.titleSmall)
        bookings.forEach { booking ->
            val row = booking.blockingRowText(serviceTitles[booking.serviceId])
            SecondaryText("$row (${booking.status.blockingLabel()})")
        }
    }
}
