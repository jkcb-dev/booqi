package corp.khin.solutions.booqi.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import corp.khin.solutions.booqi.core.designsystem.theme.BooqiCornerRadius
import corp.khin.solutions.booqi.core.designsystem.theme.BooqiSpacing
import corp.khin.solutions.booqi.core.designsystem.theme.LocalBooqiExtendedColors

/**
 * Read-only counterpart of [ReasonPicker] (docs/design/DESIGN_SYSTEM.md — the Customer sees the
 * Provider's reason on a rejection, Figma C10): the chosen reason [reason] under a [title], plus
 * the optional free-text [note]. Takes primitives only, so the design system never depends on
 * `domain`; each feature maps its own reason codes to the label.
 */
@Composable
fun ReasonSummary(
    title: String,
    reason: String,
    modifier: Modifier = Modifier,
    note: String? = null,
) {
    Card(shape = RoundedCornerShape(BooqiCornerRadius.medium), modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(BooqiSpacing.md),
            verticalArrangement = Arrangement.spacedBy(BooqiSpacing.xxs),
        ) {
            Text(title, style = MaterialTheme.typography.labelSmall, color = LocalBooqiExtendedColors.current.ink2)
            Text(reason, style = MaterialTheme.typography.bodyLarge)
            note?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
        }
    }
}
