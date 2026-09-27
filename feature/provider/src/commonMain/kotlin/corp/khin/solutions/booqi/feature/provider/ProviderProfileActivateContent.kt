package corp.khin.solutions.booqi.feature.provider

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import corp.khin.solutions.booqi.core.designsystem.theme.BooqiCornerRadius
import corp.khin.solutions.booqi.core.designsystem.theme.BooqiSpacing
import corp.khin.solutions.booqi.core.designsystem.theme.LocalBooqiExtendedColors

private val PROVIDER_MODE_BENEFITS = listOf(
    "Tu perfil visible para los Clientes en las búsquedas",
    "Podés agregar y gestionar tus propios Servicios",
    "Control total de tu horario y disponibilidad",
    "Recibí y respondé solicitudes de reserva",
)

/** P1 — "activar modo Proveedor": CTA + "¿Qué incluye?" list. */
@Composable
internal fun ActivateProviderModeContent(onAction: (ProviderProfileAction) -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(BooqiSpacing.md),
        verticalArrangement = Arrangement.spacedBy(BooqiSpacing.lg),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(BooqiSpacing.xs)) {
            Text(
                text = "Convertite en Proveedor",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = "Activá el modo Proveedor para ofrecer tus servicios y empezar a recibir reservas.",
                style = MaterialTheme.typography.bodyLarge,
                color = LocalBooqiExtendedColors.current.ink2,
            )
        }

        WhatsIncludedCard(items = PROVIDER_MODE_BENEFITS)

        Button(
            onClick = { onAction(ProviderProfileAction.ActivateProviderMode) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Activar modo Proveedor")
        }
    }
}

@Composable
private fun WhatsIncludedCard(items: List<String>) {
    Card(shape = RoundedCornerShape(BooqiCornerRadius.medium)) {
        Column(
            modifier = Modifier.padding(BooqiSpacing.md),
            verticalArrangement = Arrangement.spacedBy(BooqiSpacing.sm),
        ) {
            Text("¿Qué incluye?", style = MaterialTheme.typography.titleSmall)
            items.forEach { item ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(BooqiSpacing.xs),
                    verticalAlignment = Alignment.Top,
                ) {
                    Text(
                        text = "•",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(text = item, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}
