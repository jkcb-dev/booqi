package corp.khin.solutions.booqi.feature.provider

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import corp.khin.solutions.booqi.core.designsystem.theme.BooqiCornerRadius
import corp.khin.solutions.booqi.core.designsystem.theme.BooqiSpacing
import corp.khin.solutions.booqi.core.designsystem.theme.LocalBooqiExtendedColors

/** P2 — "completar perfil": foto, nombre profesional, descripción, ubicación. The
 * ubicación-obligatoria validation (Escenario: "El Proveedor intenta completar el perfil sin
 * ubicación") surfaces as [ProviderProfileUiState.locationError] under the field, not a crash. */
@Composable
internal fun CompleteProfileContent(
    state: ProviderProfileUiState,
    onAction: (ProviderProfileAction) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(BooqiSpacing.md)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(BooqiSpacing.md),
    ) {
        ProfileFormHeader()

        // Foto: a URL field is enough to prove the form end-to-end here; wiring a real image
        // picker/upload is role:platform-integration's scope, not compose-ui's.
        ProfileTextField(
            value = state.photoUrlInput,
            onValueChange = { onAction(ProviderProfileAction.PhotoUrlChanged(it)) },
            label = "Foto (URL)",
        )
        ProfileTextField(
            value = state.nameInput,
            onValueChange = { onAction(ProviderProfileAction.NameChanged(it)) },
            label = "Nombre profesional",
        )
        ProfileTextField(
            value = state.descriptionInput,
            onValueChange = { onAction(ProviderProfileAction.DescriptionChanged(it)) },
            label = "Descripción",
            minLines = 3,
        )
        ProfileTextField(
            value = state.locationInput,
            onValueChange = { onAction(ProviderProfileAction.LocationChanged(it)) },
            label = "Ubicación",
            errorMessage = state.locationError,
        )

        SaveProfileButton(
            isSaving = state.isSaving,
            onClick = { onAction(ProviderProfileAction.SaveProfile) },
        )
    }
}

@Composable
private fun ProfileFormHeader() {
    Column(verticalArrangement = Arrangement.spacedBy(BooqiSpacing.xs)) {
        Text("Completá tu perfil", style = MaterialTheme.typography.titleLarge)
        Text(
            text = "Nombre, foto, descripción y ubicación — así te van a encontrar los Clientes.",
            style = MaterialTheme.typography.bodyMedium,
            color = LocalBooqiExtendedColors.current.ink2,
        )
    }
}

@Composable
internal fun ProfileTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    minLines: Int = 1,
    errorMessage: String? = null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        isError = errorMessage != null,
        supportingText = errorMessage?.let { message ->
            { Text(text = message, color = MaterialTheme.colorScheme.error) }
        },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(BooqiCornerRadius.small),
        singleLine = minLines <= 1,
        minLines = minLines,
    )
}

@Composable
private fun SaveProfileButton(isSaving: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = !isSaving,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(if (isSaving) "Guardando..." else "Guardar perfil")
    }
}
