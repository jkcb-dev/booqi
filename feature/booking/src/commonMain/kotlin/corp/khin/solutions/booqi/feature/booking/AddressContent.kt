package corp.khin.solutions.booqi.feature.booking

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import corp.khin.solutions.booqi.core.common.DomainError
import corp.khin.solutions.booqi.core.designsystem.theme.BooqiSpacing
import corp.khin.solutions.booqi.core.designsystem.theme.LocalBooqiExtendedColors
import corp.khin.solutions.booqi.feature.booking.map.AddressMapPicker

private const val LOAD_ERROR = "No pudimos cargar tu dirección"
private const val DOMICILIO_HINT =
    "Necesitamos tu dirección para servicios a domicilio. Buscá por texto o mové el pin en el mapa."

/** C6: the "Mi dirección" heading and "Volver" (always visible) over a body that is a spinner, a
 * load failure with retry, or the search form. */
@Composable
internal fun AddressContent(state: AddressUiState, onAction: (AddressAction) -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        ScreenHeader(title = "Mi dirección", onBack = { onAction(AddressAction.BackClicked) })
        Box(modifier = Modifier.weight(1f)) {
            when {
                state.isLoading -> LoadingContent()
                state.loadError == DomainError.Unauthorized -> MessageContent(
                    title = "Inicia sesión",
                    message = AddressViewModel.SIGN_IN_MESSAGE,
                    actionLabel = "Volver",
                    onAction = { onAction(AddressAction.BackClicked) },
                )
                state.loadError != null -> MessageContent(
                    title = LOAD_ERROR,
                    message = state.loadError.describe(notFound = LOAD_ERROR),
                    actionLabel = "Reintentar",
                    onAction = { onAction(AddressAction.Retry) },
                )
                else -> AddressForm(state = state, onAction = onAction)
            }
        }
    }
}

@Composable
private fun AddressForm(state: AddressUiState, onAction: (AddressAction) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(BooqiSpacing.md)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(BooqiSpacing.md),
    ) {
        AddressHint(DOMICILIO_HINT)
        state.savedAddress?.let {
            LabeledValue(label = "Dirección guardada", value = it.line)
        }
        OutlinedTextField(
            value = state.query,
            onValueChange = { onAction(AddressAction.QueryChanged(it)) },
            label = { Text(if (state.savedAddress == null) "Busca tu dirección" else "Busca tu nueva dirección") },
            singleLine = true,
            enabled = !state.isSaving,
            modifier = Modifier.fillMaxWidth(),
        )
        AddressResults(state = state, onSelect = { onAction(AddressAction.ResultSelected(it)) })
        AddressMapPicker(
            cameraAddress = state.selected ?: state.savedAddress,
            enabled = !state.isSaving,
            onAddressPicked = { onAction(AddressAction.ResultSelected(it)) },
            modifier = Modifier.fillMaxWidth(),
        )
        state.saveError?.let {
            Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
        }
        Button(
            onClick = { onAction(AddressAction.SaveClicked) },
            enabled = state.canSave,
            modifier = Modifier.fillMaxWidth(),
        ) { Text(if (state.isSaving) "Guardando..." else "Guardar dirección") }
    }
}

@Composable
internal fun AddressHint(text: String, isError: Boolean = false) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = if (isError) MaterialTheme.colorScheme.error else LocalBooqiExtendedColors.current.ink2,
    )
}
