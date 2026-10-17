package corp.khin.solutions.booqi.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import corp.khin.solutions.booqi.core.designsystem.theme.BooqiCornerRadius
import corp.khin.solutions.booqi.core.designsystem.theme.BooqiSpacing

/**
 * Status badge atom (docs/design/DESIGN_SYSTEM.md "StatusBadgeES" — 7 states): a pill in the
 * status color carrying the already-localized [text]. Lives in `core:designsystem` because both
 * the Customer's history (C11) and the Provider's inbox show it.
 */
@Composable
fun StatusBadge(text: String, style: StatusBadgeStyle, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onPrimary,
        modifier = modifier
            .background(color = style.color(), shape = RoundedCornerShape(BooqiCornerRadius.pill))
            .padding(horizontal = BooqiSpacing.sm, vertical = BooqiSpacing.xxs),
    )
}
