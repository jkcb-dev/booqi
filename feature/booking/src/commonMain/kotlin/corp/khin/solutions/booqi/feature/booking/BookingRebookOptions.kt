package corp.khin.solutions.booqi.feature.booking

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import corp.khin.solutions.booqi.core.designsystem.theme.BooqiSpacing

/** C10 / C10b's two ways on: pick another slot of the same Service, or leave. */
@Composable
internal fun RebookOptions(onAction: (BookingDetailAction) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(BooqiSpacing.sm)) {
        Button(
            onClick = { onAction(BookingDetailAction.RebookClicked) },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Buscar otro horario") }
        OutlinedButton(
            onClick = { onAction(BookingDetailAction.GoHomeClicked) },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Volver al inicio") }
    }
}
