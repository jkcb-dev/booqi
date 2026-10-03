package corp.khin.solutions.booqi.feature.provider

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.input.KeyboardType
import corp.khin.solutions.booqi.core.designsystem.theme.BooqiSpacing
import corp.khin.solutions.booqi.core.designsystem.theme.LocalBooqiExtendedColors

/** The P5 input fields in Figma order: foto (Requerido), título, descripción, precio, duración,
 * modalidad. */
@Composable
internal fun ServiceEditorFields(
    state: ServiceEditorUiState,
    onAction: (ServiceEditorAction) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(BooqiSpacing.md)) {
        // Foto: a URL field, same as the #13 profile form — no image picker/upload exists yet.
        ServiceTextField(
            value = state.photoUrlInput,
            onValueChange = { onAction(ServiceEditorAction.PhotoUrlChanged(it)) },
            label = "Foto (URL) · Requerido",
            errorMessage = state.photoError,
        )
        ServiceTextField(
            value = state.titleInput,
            onValueChange = { onAction(ServiceEditorAction.TitleChanged(it)) },
            label = "Título",
        )
        ProfileTextField(
            value = state.descriptionInput,
            onValueChange = { onAction(ServiceEditorAction.DescriptionChanged(it)) },
            label = "Descripción",
            minLines = DESCRIPTION_MIN_LINES,
        )
        ServiceTextField(
            value = state.priceInput,
            onValueChange = { onAction(ServiceEditorAction.PriceChanged(it)) },
            label = "Precio",
            errorMessage = state.priceError,
            keyboardType = KeyboardType.Decimal,
        )
        ServiceTextField(
            value = state.durationInput,
            onValueChange = { onAction(ServiceEditorAction.DurationChanged(it)) },
            label = "Duración (minutos)",
            errorMessage = state.durationError,
            keyboardType = KeyboardType.Number,
        )
        Text(
            text = "Modalidad",
            style = MaterialTheme.typography.labelLarge,
            color = LocalBooqiExtendedColors.current.ink2,
        )
        ServiceModalityPicker(
            selected = state.modality,
            onSelected = { onAction(ServiceEditorAction.ModalityChanged(it)) },
        )
    }
}

private const val DESCRIPTION_MIN_LINES = 3
