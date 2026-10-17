package corp.khin.solutions.booqi.feature.booking

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import corp.khin.solutions.booqi.core.designsystem.theme.BooqiSpacing
import corp.khin.solutions.booqi.core.designsystem.theme.LocalBooqiExtendedColors

private const val EXPIRED_MESSAGE =
    "La solicitud expiró automáticamente porque el proveedor no respondió en 24 horas. " +
        "El horario volvió a estar disponible."

/** C10b — the expired request: a fixed message (there is no Provider reason) and the same options. */
@Composable
internal fun ExpiredSection(onAction: (BookingDetailAction) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(BooqiSpacing.md)) {
        Text(
            text = EXPIRED_MESSAGE,
            style = MaterialTheme.typography.bodyMedium,
            color = LocalBooqiExtendedColors.current.ink2,
        )
        RebookOptions(onAction)
    }
}
