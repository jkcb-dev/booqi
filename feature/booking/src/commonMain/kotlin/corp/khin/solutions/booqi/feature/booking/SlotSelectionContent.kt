package corp.khin.solutions.booqi.feature.booking

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import corp.khin.solutions.booqi.core.designsystem.theme.BooqiSpacing
import corp.khin.solutions.booqi.core.designsystem.theme.LocalBooqiExtendedColors
import corp.khin.solutions.booqi.domain.model.ServiceDetail

/**
 * C5 — "Seleccionar horario": the Service, the day navigator, the [TimeSlotGrid] of that day (the
 * unavailable slots struck through) and "Continuar", enabled once an available slot is chosen.
 * A day without slots (inactive weekday, block, pause) says so instead of an empty grid.
 */
@Composable
internal fun SlotSelectionContent(
    state: BookingFlowUiState,
    detail: ServiceDetail,
    onAction: (BookingFlowAction) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(BooqiSpacing.md)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(BooqiSpacing.md),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(BooqiSpacing.xxs)) {
            Text(detail.service.title, style = MaterialTheme.typography.titleMedium)
            Text(
                text = "${detail.provider.name} · ${formatDuration(detail.service.durationMinutes)} · " +
                    formatPrice(detail.service.priceCents),
                style = MaterialTheme.typography.bodyMedium,
                color = LocalBooqiExtendedColors.current.ink2,
            )
        }
        DateNavigator(
            date = state.date,
            canGoBack = state.canGoToPreviousDay,
            canGoForward = state.canGoToNextDay,
            onPrevious = { onAction(BookingFlowAction.PreviousDay) },
            onNext = { onAction(BookingFlowAction.NextDay) },
        )
        state.submitError?.let {
            Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
        }
        SlotsBody(state = state, onAction = onAction)
        Button(
            onClick = { onAction(BookingFlowAction.ContinueClicked) },
            enabled = state.selectedSlot != null,
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Continuar") }
    }
}

@Composable
private fun SlotsBody(state: BookingFlowUiState, onAction: (BookingFlowAction) -> Unit) {
    when {
        state.isLoadingSlots -> CircularProgressIndicator()
        state.slotsError != null -> {
            Text(
                text = state.slotsError.describe(notFound = "No pudimos cargar los horarios"),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
            OutlinedButton(onClick = { onAction(BookingFlowAction.Retry) }) { Text("Reintentar") }
        }
        state.slotCells.isEmpty() -> Text(
            text = "No hay horarios para este día. Prueba con otra fecha.",
            style = MaterialTheme.typography.bodyMedium,
            color = LocalBooqiExtendedColors.current.ink2,
        )
        else -> {
            if (state.slotCells.none { it.isAvailable }) {
                Text(
                    text = "Todos los horarios de este día están ocupados.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = LocalBooqiExtendedColors.current.ink2,
                )
            }
            TimeSlotGrid(
                cells = state.slotCells,
                selected = state.selectedSlot,
                onSelect = { onAction(BookingFlowAction.SlotSelected(it)) },
            )
        }
    }
}
