package corp.khin.solutions.booqi.feature.browse

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import corp.khin.solutions.booqi.core.designsystem.theme.BooqiSpacing

/** C1's text input. Typing only updates the state; the keyboard's search key or "Buscar" submits. */
@Composable
internal fun SearchField(query: String, onAction: (BrowseAction) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = BooqiSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(BooqiSpacing.sm),
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = { onAction(BrowseAction.QueryChanged(it)) },
            placeholder = { Text("¿Qué servicio buscás?") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onAction(BrowseAction.SearchSubmitted) }),
            modifier = Modifier.weight(1f),
        )
        Button(onClick = { onAction(BrowseAction.SearchSubmitted) }) { Text("Buscar") }
    }
}
