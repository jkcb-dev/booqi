package corp.khin.solutions.booqi.feature.account

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import corp.khin.solutions.booqi.core.designsystem.theme.BooqiSpacing

/**
 * The name and photo form (escenario "El Usuario actualiza su perfil"). The name is required — its
 * rejection shows under the field. The photo is a link for now: choosing a picture from the device
 * needs the platform pickers and an image loader, which belong to Platform Integration.
 */
@Composable
internal fun ProfileEditForm(state: ProfileUiState, onAction: (ProfileAction) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(BooqiSpacing.sm)) {
        AccountTextField(
            value = state.nameInput,
            onValueChange = { onAction(ProfileAction.NameChanged(it)) },
            label = "Nombre",
            errorMessage = state.nameError,
        )
        AccountTextField(
            value = state.photoUrlInput,
            onValueChange = { onAction(ProfileAction.PhotoUrlChanged(it)) },
            label = "Enlace de tu foto (opcional)",
            kind = AccountFieldKind.Url,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(BooqiSpacing.sm), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = { onAction(ProfileAction.CancelEditClicked) },
                enabled = !state.isSaving,
                modifier = Modifier.weight(1f),
            ) { Text("Cancelar") }
            Button(
                onClick = { onAction(ProfileAction.SaveClicked) },
                enabled = !state.isSaving,
                modifier = Modifier.weight(1f),
            ) { Text(if (state.isSaving) "Guardando..." else "Guardar") }
        }
    }
}
