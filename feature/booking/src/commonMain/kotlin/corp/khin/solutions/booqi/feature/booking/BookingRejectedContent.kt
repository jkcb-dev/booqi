package corp.khin.solutions.booqi.feature.booking

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import corp.khin.solutions.booqi.core.designsystem.component.ReasonSummary
import corp.khin.solutions.booqi.core.designsystem.theme.BooqiSpacing
import corp.khin.solutions.booqi.domain.model.Booking

/** C10 — the rejected request: the Provider's reason (read-only) and the rebook / home options. */
@Composable
internal fun RejectedSection(booking: Booking, onAction: (BookingDetailAction) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(BooqiSpacing.md)) {
        booking.reason?.let {
            ReasonSummary(title = "Motivo del rechazo", reason = it.label(), note = it.note)
        }
        RebookOptions(onAction)
    }
}
