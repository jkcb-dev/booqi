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
 * Modality badge atom (docs/design/DESIGN_SYSTEM.md — Local / Domicilio / Local & Dom.). Lives in
 * `core:designsystem` because both `feature:provider` (P4 service list) and `feature:browse`
 * (C3/C4 service cards) need it. Takes the already-localized [text] rather than a domain
 * `ServiceModality` so the design system never depends on `domain`; each feature maps its own
 * modality to the label.
 */
@Composable
fun ModalityBadge(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onPrimaryContainer,
        modifier = modifier
            .background(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(BooqiCornerRadius.pill),
            )
            .padding(horizontal = BooqiSpacing.sm, vertical = BooqiSpacing.xxs),
    )
}
