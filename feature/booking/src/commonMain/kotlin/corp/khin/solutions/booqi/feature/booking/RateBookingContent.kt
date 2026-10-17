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

private const val LOAD_ERROR = "No pudimos cargar la reserva"

/** C12: the "Calificar cita" heading and "Volver" (always visible) over a body that is a spinner,
 * "Reserva no encontrada", a load failure with retry, the reason a Booking cannot be rated, or the
 * form. */
@Composable
internal fun RateBookingContent(state: RateBookingUiState, onAction: (RateBookingAction) -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        ScreenHeader(title = "Calificar cita", onBack = { onAction(RateBookingAction.BackClicked) })
        Box(modifier = Modifier.weight(1f)) {
            when {
                state.isLoading -> LoadingContent()
                state.error == DomainError.NotFound -> MessageContent(
                    title = "Reserva no encontrada",
                    message = "Es posible que ya no exista.",
                    actionLabel = "Volver",
                    onAction = { onAction(RateBookingAction.BackClicked) },
                )
                state.error != null -> MessageContent(
                    title = LOAD_ERROR,
                    message = state.error.describe(notFound = LOAD_ERROR),
                    actionLabel = "Reintentar",
                    onAction = { onAction(RateBookingAction.Retry) },
                )
                state.notRatableReason != null -> MessageContent(
                    title = "No se puede calificar",
                    message = state.notRatableReason.orEmpty(),
                    actionLabel = "Volver",
                    onAction = { onAction(RateBookingAction.BackClicked) },
                )
                state.booking != null -> RateBookingForm(state = state, onAction = onAction)
                else -> LoadingContent()
            }
        }
    }
}

@Composable
private fun RateBookingForm(state: RateBookingUiState, onAction: (RateBookingAction) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(BooqiSpacing.md)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(BooqiSpacing.md),
    ) {
        Text(state.serviceTitle ?: SERVICE_FALLBACK_LABEL, style = MaterialTheme.typography.titleMedium)
        state.booking?.let {
            Text(
                text = it.whenText(),
                style = MaterialTheme.typography.bodyMedium,
                color = LocalBooqiExtendedColors.current.ink2,
            )
        }
        Text("¿Cómo fue tu experiencia?", style = MaterialTheme.typography.titleSmall)
        StarSelector(
            stars = state.stars,
            enabled = !state.isSubmitting,
            onSelect = { onAction(RateBookingAction.StarsSelected(it)) },
        )
        OutlinedTextField(
            value = state.comment,
            onValueChange = { onAction(RateBookingAction.CommentChanged(it)) },
            label = { Text("Comentario (opcional)") },
            enabled = !state.isSubmitting,
            modifier = Modifier.fillMaxWidth(),
        )
        state.formError?.let {
            Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
        }
        Button(
            onClick = { onAction(RateBookingAction.Submit) },
            enabled = state.canSubmit,
            modifier = Modifier.fillMaxWidth(),
        ) { Text(if (state.isSubmitting) "Enviando..." else "Enviar calificación") }
    }
}
