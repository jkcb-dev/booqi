package corp.khin.solutions.booqi.feature.booking.map

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** Shared confirm control under the platform map (keeps each `actual` under detekt LongMethod). */
@Composable
internal fun MapPinConfirmSection(
    enabled: Boolean,
    isResolving: Boolean,
    resolveError: String?,
    onConfirm: () -> Unit,
) {
    resolveError?.let {
        Text(
            text = it,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
        )
    }
    OutlinedButton(
        onClick = onConfirm,
        enabled = enabled && !isResolving,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(if (isResolving) MapPinFallback.RESOLVING_LABEL else MapPinFallback.CONFIRM_LABEL)
    }
}
