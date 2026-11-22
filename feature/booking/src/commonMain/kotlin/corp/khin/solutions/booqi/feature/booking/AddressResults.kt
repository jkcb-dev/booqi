package corp.khin.solutions.booqi.feature.booking

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import corp.khin.solutions.booqi.core.designsystem.theme.BooqiCornerRadius
import corp.khin.solutions.booqi.core.designsystem.theme.BooqiSpacing
import corp.khin.solutions.booqi.domain.model.Address
import corp.khin.solutions.booqi.domain.usecase.BuscarDireccionUseCase

/** The answer under the search field: a hint while the query is short, a progress note, a failure,
 * "no results", or the candidates, the selected one highlighted. */
@Composable
internal fun AddressResults(state: AddressUiState, onSelect: (Address) -> Unit) {
    when {
        state.searchError != null ->
            AddressHint(state.searchError.describe(notFound = "No pudimos buscar"), isError = true)
        state.isSearching -> AddressHint("Buscando...")
        state.results.isNotEmpty() -> Column(verticalArrangement = Arrangement.spacedBy(BooqiSpacing.xs)) {
            state.results.forEach { address ->
                AddressResultCard(
                    address = address,
                    isSelected = address == state.selected,
                    onClick = { onSelect(address) },
                )
            }
        }
        state.hasSearched -> AddressHint("No encontramos direcciones. Prueba con otra búsqueda.")
        state.selected == null ->
            AddressHint("Escribe al menos ${BuscarDireccionUseCase.MIN_QUERY_LENGTH} letras de tu dirección.")
    }
    state.selected?.takeIf { it !in state.results }?.let {
        LabeledValue(label = "Dirección elegida", value = it.line)
    }
}

@Composable
private fun AddressResultCard(address: Address, isSelected: Boolean, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(BooqiCornerRadius.medium),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.secondaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            },
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = address.line,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(BooqiSpacing.md),
        )
    }
}
