package corp.khin.solutions.booqi.feature.booking

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import corp.khin.solutions.booqi.core.designsystem.theme.BooqiSpacing
import corp.khin.solutions.booqi.core.designsystem.theme.LocalBooqiExtendedColors

/**
 * C7's address block for an appointment at the Customer's home (docs/domain/customer-flow.md
 * § Grupo 2): the saved address is used as is ("sin pedirla de nuevo"); without one a clear
 * blocking state offers "Agregar dirección" (the address screen is #23, reached through the
 * `onAddAddress` callback); a visitor is told to sign in, which sending will ask for.
 */
@Composable
internal fun BookingAddressSection(state: BookingFlowUiState, onAction: (BookingFlowAction) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(BooqiSpacing.xs)) {
        when (state.addressLookup) {
            AddressLookup.PENDING -> Muted("Buscando tu dirección guardada...")
            AddressLookup.FOUND -> Text(
                text = "Usaremos tu dirección guardada: ${state.savedAddress?.line.orEmpty()}",
                style = MaterialTheme.typography.bodyMedium,
            )
            AddressLookup.MISSING -> {
                Text(
                    text = "Este servicio es a domicilio y no tienes una dirección guardada. " +
                        "Agrégala para poder enviar la solicitud.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
                Button(
                    onClick = { onAction(BookingFlowAction.AddAddressClicked) },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Agregar dirección") }
            }
            AddressLookup.SIGNED_OUT -> Muted("Inicia sesión para usar tu dirección guardada.")
            AddressLookup.FAILED -> {
                Text(
                    text = "No pudimos leer tu dirección guardada.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
                OutlinedButton(onClick = { onAction(BookingFlowAction.RetryAddress) }) { Text("Reintentar") }
            }
        }
    }
}

@Composable
private fun Muted(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = LocalBooqiExtendedColors.current.ink2)
}
