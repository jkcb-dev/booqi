package corp.khin.solutions.booqi.feature.booking

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import corp.khin.solutions.booqi.core.designsystem.theme.BooqiCornerRadius
import corp.khin.solutions.booqi.core.designsystem.theme.BooqiSpacing

/**
 * What the Identity guard answered when the Customer tried to send (docs/domain/identity-flow.md:
 * "se le pide registrarse o iniciar sesión" / "debe verificar su correo"). The button re-fires the
 * hand-off to the sign-in / account screen in case the first one was dismissed.
 */
@Composable
internal fun BookingAccessNotice(issue: AccessIssue, onAction: (BookingFlowAction) -> Unit) {
    val (message, action) = when (issue) {
        AccessIssue.SIGN_IN_REQUIRED -> "Inicia sesión o regístrate para enviar la solicitud." to "Iniciar sesión"
        AccessIssue.EMAIL_NOT_VERIFIED ->
            "Verifica tu correo para enviar la solicitud. Puedes reenviar el correo desde tu cuenta." to
                "Ir a mi cuenta"
    }
    Card(
        shape = RoundedCornerShape(BooqiCornerRadius.medium),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(BooqiSpacing.md),
            verticalArrangement = Arrangement.spacedBy(BooqiSpacing.sm),
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            OutlinedButton(onClick = { onAction(BookingFlowAction.SignInClicked) }) { Text(action) }
        }
    }
}
