package corp.khin.solutions.booqi.feature.booking

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import kotlinx.datetime.LocalDate

/** C5's day navigation: "Anterior" / the date / "Siguiente". The ends are disabled (no past days,
 * and a booking horizon). */
@Composable
internal fun DateNavigator(
    date: LocalDate,
    canGoBack: Boolean,
    canGoForward: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        TextButton(onClick = onPrevious, enabled = canGoBack) { Text("Anterior") }
        Text(
            text = date.spanishLongText(),
            style = MaterialTheme.typography.titleSmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f),
        )
        TextButton(onClick = onNext, enabled = canGoForward) { Text("Siguiente") }
    }
}
