package corp.khin.solutions.booqi.feature.provider

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import corp.khin.solutions.booqi.core.designsystem.theme.BooqiCornerRadius
import corp.khin.solutions.booqi.core.designsystem.theme.BooqiSpacing
import corp.khin.solutions.booqi.domain.model.BookingStatus

/** A booking's status in its Figma color (docs/design/DESIGN_SYSTEM.md "StatusBadgeES"). Private
 * to this module until a second feature needs it, at which point it is promoted to
 * `core:designsystem`. */
@Composable
internal fun BookingStatusChip(status: BookingStatus) {
    Text(
        text = status.label(),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onPrimary,
        modifier = Modifier
            .background(color = status.color(), shape = RoundedCornerShape(BooqiCornerRadius.pill))
            .padding(horizontal = BooqiSpacing.sm, vertical = BooqiSpacing.xxs),
    )
}
