package corp.khin.solutions.booqi.feature.provider

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import corp.khin.solutions.booqi.core.common.DomainError
import corp.khin.solutions.booqi.core.designsystem.theme.BooqiSpacing
import corp.khin.solutions.booqi.core.designsystem.theme.LocalBooqiExtendedColors

private const val LOAD_ERROR_MESSAGE = "No pudimos cargar tus reservas"

/** The body of the active tab: a spinner, the load-failure retry, the tab's empty state, or its
 * list. A refresh over an already-shown list keeps the list on screen instead of blanking it. */
@Composable
internal fun BookingListContent(state: BookingInboxUiState, onAction: (BookingInboxAction) -> Unit) {
    val isRequests = state.tab == BookingInboxTab.REQUESTS
    val bookings = if (isRequests) state.requests else state.confirmed
    Box(modifier = Modifier.fillMaxSize()) {
        when {
            state.isLoading && bookings.isEmpty() -> CenteredColumn { CircularProgressIndicator() }
            state.loadError != null && bookings.isEmpty() -> LoadErrorContent(state.loadError, onAction)
            bookings.isEmpty() -> EmptyBookingsContent(isRequests)
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(BooqiSpacing.md),
                verticalArrangement = Arrangement.spacedBy(BooqiSpacing.md),
            ) {
                items(bookings, key = { it.id }) { booking ->
                    BookingListItem(
                        booking = booking,
                        serviceTitle = state.serviceTitles[booking.serviceId] ?: SERVICE_FALLBACK_LABEL,
                        showRequestActions = isRequests,
                        isSubmitting = state.submittingBookingId != null,
                        onAction = onAction,
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyBookingsContent(isRequests: Boolean) {
    CenteredColumn {
        Text(
            text = if (isRequests) "No tenés solicitudes pendientes" else "No tenés citas confirmadas",
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = if (isRequests) {
                "Cuando un Cliente pida una reserva, la verás acá."
            } else {
                "Las solicitudes que aceptes aparecerán acá."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = LocalBooqiExtendedColors.current.ink2,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun LoadErrorContent(error: DomainError, onAction: (BookingInboxAction) -> Unit) {
    CenteredColumn {
        Text(LOAD_ERROR_MESSAGE, style = MaterialTheme.typography.titleMedium)
        Text(
            text = error.describe(notFound = LOAD_ERROR_MESSAGE),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center,
        )
        OutlinedButton(onClick = { onAction(BookingInboxAction.Refresh) }) { Text("Reintentar") }
    }
}
