package corp.khin.solutions.booqi.feature.browse

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import corp.khin.solutions.booqi.core.common.DomainError

private const val LOAD_ERROR_TITLE = "No pudimos cargar el perfil"

/** C4: the "Perfil del proveedor" heading and "Volver" (always visible) over a body that is a
 * spinner, "Proveedor no encontrado", a load failure with retry, or the profile. */
@Composable
internal fun ProviderPublicProfileContent(
    state: ProviderPublicProfileUiState,
    onAction: (ProviderPublicProfileAction) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        DetailHeader(title = "Perfil del proveedor", onBack = { onAction(ProviderPublicProfileAction.BackClicked) })
        Box(modifier = Modifier.weight(1f)) {
            val profile = state.profile
            when {
                state.isLoading -> LoadingContent()
                state.error == DomainError.NotFound -> MessageContent(
                    title = "Proveedor no encontrado",
                    message = "Es posible que ya no esté disponible.",
                    actionLabel = "Volver",
                    onAction = { onAction(ProviderPublicProfileAction.BackClicked) },
                )
                state.error != null -> MessageContent(
                    title = LOAD_ERROR_TITLE,
                    message = state.error.describe(notFound = LOAD_ERROR_TITLE),
                    actionLabel = "Reintentar",
                    onAction = { onAction(ProviderPublicProfileAction.Retry) },
                )
                profile != null -> ProviderPublicProfileBody(profile = profile, onAction = onAction)
                else -> LoadingContent()
            }
        }
    }
}
