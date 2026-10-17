package corp.khin.solutions.booqi.feature.booking

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import corp.khin.solutions.booqi.core.common.DomainError

private const val LOAD_ERROR = "No pudimos cargar la reserva"

/** The heading of each variant (C8-C10b); loading and error states keep a generic one. */
private fun BookingDetailVariant?.title(): String = when (this) {
    BookingDetailVariant.PENDING -> "Solicitud pendiente"
    BookingDetailVariant.CONFIRMED -> "Cita confirmada"
    BookingDetailVariant.REJECTED -> "Solicitud rechazada"
    BookingDetailVariant.EXPIRED -> "Solicitud expirada"
    BookingDetailVariant.COMPLETED -> "Cita completada"
    BookingDetailVariant.CANCELLED -> "Reserva cancelada"
    null -> "Detalle de la reserva"
}

/** The detail's frame: the variant's heading and "Volver" (always visible) over a body that is a
 * spinner, "Reserva no encontrada", a load failure with retry, or the Booking itself. */
@Composable
internal fun BookingDetailContent(state: BookingDetailUiState, onAction: (BookingDetailAction) -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        ScreenHeader(title = state.variant.title(), onBack = { onAction(BookingDetailAction.BackClicked) })
        Box(modifier = Modifier.weight(1f)) {
            val booking = state.booking
            when {
                state.isLoading -> LoadingContent()
                state.error == DomainError.NotFound -> MessageContent(
                    title = "Reserva no encontrada",
                    message = "Es posible que ya no exista.",
                    actionLabel = "Volver",
                    onAction = { onAction(BookingDetailAction.BackClicked) },
                )
                state.error != null -> MessageContent(
                    title = LOAD_ERROR,
                    message = state.error.describe(notFound = LOAD_ERROR),
                    actionLabel = "Reintentar",
                    onAction = { onAction(BookingDetailAction.Retry) },
                )
                booking != null -> BookingDetailBody(state = state, onAction = onAction)
                else -> LoadingContent()
            }
        }
    }
}
