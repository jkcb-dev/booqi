package corp.khin.solutions.booqi.feature.browse

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import corp.khin.solutions.booqi.core.designsystem.theme.BooqiCornerRadius
import corp.khin.solutions.booqi.core.designsystem.theme.BooqiSpacing
import corp.khin.solutions.booqi.core.designsystem.theme.LocalBooqiExtendedColors

/** The heading (visible in every state) and "Volver" of the C3/C4 screens, over their body. */
@Composable
internal fun DetailHeader(title: String, onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = BooqiSpacing.md, top = BooqiSpacing.md, end = BooqiSpacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
        TextButton(onClick = onBack) { Text("Volver") }
    }
}

@Composable
internal fun CenteredColumn(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(BooqiSpacing.md),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(BooqiSpacing.sm, Alignment.CenterVertically),
    ) { content() }
}

@Composable
internal fun LoadingContent() = CenteredColumn { CircularProgressIndicator() }

/** A centered [title] and [message] with one outlined action: a load failure ("Reintentar") or a
 * "no encontrado" ("Volver"). */
@Composable
internal fun MessageContent(title: String, message: String, actionLabel: String, onAction: () -> Unit) {
    CenteredColumn {
        Text(title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = LocalBooqiExtendedColors.current.ink2,
            textAlign = TextAlign.Center,
        )
        OutlinedButton(onClick = onAction) { Text(actionLabel) }
    }
}

/**
 * Stand-in for a Service's/Provider's photo: the first letter on a brand tint. There is no image
 * loader in the project yet (choosing one is Architect's call), so the `photoUrl`s are not
 * rendered; every photo slot goes through here so swapping in real images touches one place.
 */
@Composable
internal fun PhotoPlaceholder(name: String, modifier: Modifier = Modifier) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .clip(RoundedCornerShape(BooqiCornerRadius.small))
            .background(MaterialTheme.colorScheme.primaryContainer),
    ) {
        Text(
            text = name.take(1).uppercase(),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    }
}
