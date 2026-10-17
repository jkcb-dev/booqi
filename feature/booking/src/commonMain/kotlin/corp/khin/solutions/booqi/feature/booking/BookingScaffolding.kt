package corp.khin.solutions.booqi.feature.booking

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import corp.khin.solutions.booqi.core.designsystem.theme.BooqiSpacing
import corp.khin.solutions.booqi.core.designsystem.theme.LocalBooqiExtendedColors

/** The heading (visible in every state) and the back button of a booking screen, over its body. */
@Composable
internal fun ScreenHeader(title: String, onBack: () -> Unit, backLabel: String = "Volver") {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = BooqiSpacing.md, top = BooqiSpacing.md, end = BooqiSpacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
        TextButton(onClick = onBack) { Text(backLabel) }
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

/** A small caption over its value, the way every fact of a booking is listed. */
@Composable
internal fun LabeledValue(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(BooqiSpacing.xxs)) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = LocalBooqiExtendedColors.current.ink2)
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}
