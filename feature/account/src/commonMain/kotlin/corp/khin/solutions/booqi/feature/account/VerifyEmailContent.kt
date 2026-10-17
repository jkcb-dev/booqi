package corp.khin.solutions.booqi.feature.account

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** The verification notice: where the email went, "Ya verifiqué", "Reenviar" and "Más tarde". */
@Composable
internal fun VerifyEmailContent(state: VerifyEmailUiState, onAction: (VerifyEmailAction) -> Unit) {
    AccountScreenBody(title = "Verificá tu correo") {
        when {
            state.isLoading -> LoadingIndicator()
            state.email == null && state.error != null ->
                LoadFailure(state.error.describe(), onRetry = { onAction(VerifyEmailAction.Start) })
            else -> VerifyEmailActions(state, onAction)
        }
    }
}

@Composable
private fun VerifyEmailActions(state: VerifyEmailUiState, onAction: (VerifyEmailAction) -> Unit) {
    val busy = state.isChecking || state.isResending
    SecondaryText(
        text = "Para reservar o convertirte en Proveedor necesitamos confirmar tu correo" +
            (state.email?.let { ": $it" } ?: "") +
            ". Abrí el enlace del mensaje que te enviamos.",
    )
    state.notice?.let {
        Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
    }
    state.error?.let { ErrorText(it.describe()) }
    Button(
        onClick = { onAction(VerifyEmailAction.ConfirmClicked) },
        enabled = !busy,
        modifier = Modifier.fillMaxWidth(),
    ) { Text(if (state.isChecking) "Verificando..." else "Ya verifiqué mi correo") }
    OutlinedButton(
        onClick = { onAction(VerifyEmailAction.ResendClicked) },
        enabled = !busy,
        modifier = Modifier.fillMaxWidth(),
    ) { Text(if (state.isResending) "Enviando..." else "Reenviar correo de verificación") }
    TextButton(onClick = { onAction(VerifyEmailAction.LaterClicked) }, modifier = Modifier.fillMaxWidth()) {
        Text("Más tarde")
    }
}
