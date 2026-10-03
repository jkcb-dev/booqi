package corp.khin.solutions.booqi.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import corp.khin.solutions.booqi.core.designsystem.theme.BooqiSpacing
import corp.khin.solutions.booqi.core.designsystem.theme.LocalBooqiExtendedColors

/**
 * Empty-state organism (docs/design/DESIGN_SYSTEM.md — title + description + optional CTA; the
 * four confirmed variants are sin servicios, sin solicitudes, sin reservas, sin resultados). Lives
 * in `core:designsystem` because every `feature:*` module needs one. Takes only text and an
 * optional [action] slot (the CTA button, built by the caller so it owns its behavior), so the
 * design system never depends on `domain`. There is no icon yet: the icon set is not chosen.
 *
 * Centers itself in the space it is given, so pass `Modifier.fillMaxSize()` to centre it in a
 * screen body.
 */
@Composable
fun EmptyState(
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null,
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(BooqiSpacing.md),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(BooqiSpacing.sm, Alignment.CenterVertically),
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = LocalBooqiExtendedColors.current.ink2,
            textAlign = TextAlign.Center,
        )
        action?.invoke()
    }
}
