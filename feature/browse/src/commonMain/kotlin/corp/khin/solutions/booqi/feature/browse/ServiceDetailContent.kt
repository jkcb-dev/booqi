package corp.khin.solutions.booqi.feature.browse

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import corp.khin.solutions.booqi.core.common.DomainError

private const val LOAD_ERROR_TITLE = "No pudimos cargar el servicio"

/** C3: the "Detalle del servicio" heading and "Volver" (always visible) over a body that is a
 * spinner, "Servicio no encontrado", a load failure with retry, or the detail. */
@Composable
internal fun ServiceDetailContent(state: ServiceDetailUiState, onAction: (ServiceDetailAction) -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        DetailHeader(title = "Detalle del servicio", onBack = { onAction(ServiceDetailAction.BackClicked) })
        Box(modifier = Modifier.weight(1f)) {
            val detail = state.detail
            when {
                state.isLoading -> LoadingContent()
                state.error == DomainError.NotFound -> MessageContent(
                    title = "Servicio no encontrado",
                    message = "Es posible que ya no esté disponible.",
                    actionLabel = "Volver",
                    onAction = { onAction(ServiceDetailAction.BackClicked) },
                )
                state.error != null -> MessageContent(
                    title = LOAD_ERROR_TITLE,
                    message = state.error.describe(notFound = LOAD_ERROR_TITLE),
                    actionLabel = "Reintentar",
                    onAction = { onAction(ServiceDetailAction.Retry) },
                )
                detail != null -> ServiceDetailBody(detail = detail, onAction = onAction)
                else -> LoadingContent()
            }
        }
    }
}
