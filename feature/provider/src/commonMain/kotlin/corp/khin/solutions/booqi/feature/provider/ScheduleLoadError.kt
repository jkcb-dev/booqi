package corp.khin.solutions.booqi.feature.provider

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.style.TextAlign
import corp.khin.solutions.booqi.core.common.DomainError

private const val LOAD_ERROR_MESSAGE = "No pudimos cargar tu horario"

/** The body of either P6/P7 tab when the schedule could not be loaded: message + retry. */
@Composable
internal fun ScheduleLoadError(error: DomainError, onRetry: () -> Unit) {
    CenteredColumn {
        Text(LOAD_ERROR_MESSAGE, style = MaterialTheme.typography.titleMedium)
        Text(
            text = error.describe(notFound = LOAD_ERROR_MESSAGE),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center,
        )
        OutlinedButton(onClick = onRetry) { Text("Reintentar") }
    }
}
