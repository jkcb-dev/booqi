package corp.khin.solutions.booqi.core.designsystem.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import corp.khin.solutions.booqi.core.designsystem.theme.LocalBooqiExtendedColors

/**
 * The seven booking-status looks of `StatusBadgeES` (docs/design/DESIGN_SYSTEM.md status table).
 * The design system never depends on `domain`, so each feature maps its own `BookingStatus` to one
 * of these and supplies the Spanish label.
 */
enum class StatusBadgeStyle {
    CONFIRMADA,
    PENDIENTE,
    RECHAZADA,
    COMPLETADA,
    EXPIRADA,
    CANCELADA_PROVEEDOR,
    CANCELADA_CLIENTE,
}

/** The Figma color of [this] style, read from the theme's extended tokens. */
@Composable
fun StatusBadgeStyle.color(): Color {
    val colors = LocalBooqiExtendedColors.current
    return when (this) {
        StatusBadgeStyle.CONFIRMADA -> colors.statusConfirmada
        StatusBadgeStyle.PENDIENTE -> colors.statusPendiente
        StatusBadgeStyle.RECHAZADA -> colors.statusRechazada
        StatusBadgeStyle.COMPLETADA -> colors.statusCompletada
        StatusBadgeStyle.EXPIRADA -> colors.statusExpirada
        StatusBadgeStyle.CANCELADA_PROVEEDOR -> colors.statusCanceladaProveedor
        StatusBadgeStyle.CANCELADA_CLIENTE -> colors.statusCanceladaCliente
    }
}
