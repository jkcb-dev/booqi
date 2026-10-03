package corp.khin.solutions.booqi.feature.provider

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import corp.khin.solutions.booqi.core.designsystem.theme.BooqiSpacing

/** P5 form: foto (marcada Requerido), título, descripción, precio, duración, modalidad. The
 * foto-obligatoria validation (Escenario: "El Proveedor agrega un Servicio sin foto") renders
 * under the photo field as [ServiceEditorUiState.photoError]. */
@Composable
internal fun ServiceEditorFormContent(
    state: ServiceEditorUiState,
    onAction: (ServiceEditorAction) -> Unit,
    onCancel: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(BooqiSpacing.md)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(BooqiSpacing.md),
    ) {
        Text(
            text = if (state.isEditing) "Editar servicio" else "Agregar servicio",
            style = MaterialTheme.typography.titleLarge,
        )
        state.error?.let { error ->
            Text(
                text = error.describe(notFound = "No se encontró el servicio"),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
        }

        ServiceEditorFields(state = state, onAction = onAction)

        Button(
            onClick = { onAction(ServiceEditorAction.Save) },
            enabled = !state.isSaving,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (state.isSaving) "Guardando..." else "Guardar servicio")
        }
        OutlinedButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) { Text("Cancelar") }
    }
}
