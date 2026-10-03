package corp.khin.solutions.booqi.feature.provider

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import corp.khin.solutions.booqi.core.designsystem.theme.BooqiCornerRadius
import corp.khin.solutions.booqi.core.designsystem.theme.BooqiSpacing

/** The outcome of the last action over the lists ("Solicitud aceptada"), or why a row action
 * failed, with a way to dismiss it. Stands in for a snackbar, which `core:designsystem` lacks. */
@Composable
internal fun NoticeBanner(notice: BookingNotice, onDismiss: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val container = if (notice.isError) colors.error else colors.primaryContainer
    val content = if (notice.isError) colors.onError else colors.onPrimaryContainer
    Card(
        shape = RoundedCornerShape(BooqiCornerRadius.medium),
        colors = CardDefaults.cardColors(containerColor = container, contentColor = content),
        modifier = Modifier.fillMaxWidth().padding(horizontal = BooqiSpacing.md, vertical = BooqiSpacing.xs),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = BooqiSpacing.md, end = BooqiSpacing.xs),
        ) {
            Text(notice.message, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            TextButton(onClick = onDismiss) { Text("Cerrar", color = content) }
        }
    }
}
