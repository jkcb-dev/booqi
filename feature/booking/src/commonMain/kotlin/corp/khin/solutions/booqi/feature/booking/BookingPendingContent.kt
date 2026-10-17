package corp.khin.solutions.booqi.feature.booking

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import corp.khin.solutions.booqi.core.designsystem.theme.LocalBooqiExtendedColors

/** C8 — the pending request: what happens next. The clock glyph, "Pendiente" badge and the 24h
 * countdown are the hero above ([BookingDetailBody]). */
@Composable
internal fun PendingSection() {
    Text(
        text = "Te avisaremos cuando el proveedor acepte o rechace tu solicitud. " +
            "Si no responde en 24 horas, la solicitud expirará.",
        style = MaterialTheme.typography.bodyMedium,
        color = LocalBooqiExtendedColors.current.ink2,
    )
}
