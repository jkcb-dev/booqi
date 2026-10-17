package corp.khin.solutions.booqi.feature.booking

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import corp.khin.solutions.booqi.core.designsystem.component.EmptyState
import corp.khin.solutions.booqi.core.designsystem.component.StatusBadge
import corp.khin.solutions.booqi.core.designsystem.theme.BooqiCornerRadius
import corp.khin.solutions.booqi.core.designsystem.theme.BooqiSpacing
import corp.khin.solutions.booqi.core.designsystem.theme.LocalBooqiExtendedColors
import corp.khin.solutions.booqi.domain.model.Booking

/** The selected tab's Bookings; a tab without any says so instead of showing an empty list. */
@Composable
internal fun BookingListContent(state: MyBookingsUiState, onAction: (MyBookingsAction) -> Unit) {
    val bookings = state.visibleBookings
    if (bookings.isEmpty()) {
        EmptyState(
            title = "Sin reservas ${state.tab.emptyLabel()}",
            description = "Prueba con otra pestaña.",
            modifier = Modifier.fillMaxSize(),
        )
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(BooqiSpacing.md),
        verticalArrangement = Arrangement.spacedBy(BooqiSpacing.sm),
    ) {
        items(bookings, key = { it.id }) { booking ->
            BookingListItem(
                booking = booking,
                title = state.titles[booking.serviceId] ?: SERVICE_FALLBACK_LABEL,
                state = state,
                onClick = { onAction(MyBookingsAction.BookingClicked(booking.id)) },
            )
        }
    }
}

private fun BookingsTab.emptyLabel(): String = when (this) {
    BookingsTab.ALL -> ""
    BookingsTab.CONFIRMED -> "confirmadas"
    BookingsTab.REQUESTED -> "pendientes"
    BookingsTab.COMPLETED -> "completadas"
}.trim()

@Composable
private fun BookingListItem(booking: Booking, title: String, state: MyBookingsUiState, onClick: () -> Unit) {
    val status = booking.effectiveStatus(state.now)
    Card(
        shape = RoundedCornerShape(BooqiCornerRadius.medium),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier.padding(BooqiSpacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(BooqiSpacing.sm),
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(BooqiSpacing.xxs)) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Text(
                    text = booking.whenText(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = LocalBooqiExtendedColors.current.ink2,
                )
                Text(booking.priceText(), style = MaterialTheme.typography.bodyMedium)
            }
            StatusBadge(text = status.customerLabel(), style = status.badgeStyle())
        }
    }
}
