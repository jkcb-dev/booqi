package corp.khin.solutions.booqi.feature.booking

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * The request flow's frame: the step's heading and "Volver" (always visible) over a body that is a
 * spinner, "Servicio no encontrado", a load failure with retry, or the step's content (C5 / C7).
 */
@Composable
internal fun BookingFlowContent(state: BookingFlowUiState, onAction: (BookingFlowAction) -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        ScreenHeader(
            title = if (state.step == BookingStep.CONFIRM) "Confirmar reserva" else "Elige un horario",
            onBack = { onAction(BookingFlowAction.BackClicked) },
        )
        Box(modifier = Modifier.weight(1f)) {
            val detail = state.detail
            when {
                state.isLoading -> LoadingContent()
                state.loadError.isNotFound() -> MessageContent(
                    title = SERVICE_NOT_FOUND_TITLE,
                    message = "Es posible que ya no esté disponible.",
                    actionLabel = "Volver",
                    onAction = { onAction(BookingFlowAction.BackClicked) },
                )
                state.loadError != null -> MessageContent(
                    title = LOAD_ERROR_TITLE,
                    message = state.loadError.describe(notFound = LOAD_ERROR_TITLE),
                    actionLabel = "Reintentar",
                    onAction = { onAction(BookingFlowAction.Retry) },
                )
                detail == null -> LoadingContent()
                state.step == BookingStep.SELECT_SLOT -> SlotSelectionContent(state, detail, onAction)
                else -> BookingConfirmContent(state, detail, onAction)
            }
        }
    }
}
