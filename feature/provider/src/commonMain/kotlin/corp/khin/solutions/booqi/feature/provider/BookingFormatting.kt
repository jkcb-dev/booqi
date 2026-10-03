package corp.khin.solutions.booqi.feature.provider

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import corp.khin.solutions.booqi.core.designsystem.component.ReasonOption
import corp.khin.solutions.booqi.core.designsystem.theme.LocalBooqiExtendedColors
import corp.khin.solutions.booqi.domain.model.Booking
import corp.khin.solutions.booqi.domain.model.BookingStatus
import corp.khin.solutions.booqi.domain.model.ProviderReasonCode
import corp.khin.solutions.booqi.domain.model.Reason

/**
 * What every row and the detail call the Customer. There is no Identity context yet, so a Booking
 * only holds a `customerId` and no display name exists to show; replace with the real name once
 * Identity lands (same for reviewers on the received-ratings list).
 */
internal const val CLIENT_LABEL = "Cliente"

/** Shown when a Booking's service title can't be resolved. */
internal const val SERVICE_FALLBACK_LABEL = "Servicio"

/** `$12.50` — the price agreed at request time, formatted like the Servicios list (the domain has
 * no currency). */
internal fun Booking.priceText(): String = "\$${formatPriceInput(priceCentsSnapshot)}"

/** `25 de diciembre de 2026 · 09:00` — the appointment's provider-local date and start time. */
internal fun Booking.whenText(): String =
    "${scheduledAt.date.spanishText()} · ${formatTime(scheduledAt.time)}"

/** The Spanish badge text of each [BookingStatus] (docs/design/DESIGN_SYSTEM.md status table). */
internal fun BookingStatus.label(): String = when (this) {
    BookingStatus.REQUESTED -> "Pendiente"
    BookingStatus.CONFIRMED -> "Confirmada"
    BookingStatus.COMPLETED -> "Completada"
    BookingStatus.REJECTED -> "Rechazada"
    BookingStatus.EXPIRED -> "Expirada"
    BookingStatus.CANCELLED_BY_PROVIDER -> "Cancelada por ti"
    BookingStatus.CANCELLED_BY_CUSTOMER -> "Cancelada por el cliente"
}

/** The Figma status color of each [BookingStatus], read from the theme's extended tokens. */
@Composable
internal fun BookingStatus.color(): Color {
    val colors = LocalBooqiExtendedColors.current
    return when (this) {
        BookingStatus.REQUESTED -> colors.statusPendiente
        BookingStatus.CONFIRMED -> colors.statusConfirmada
        BookingStatus.COMPLETED -> colors.statusCompletada
        BookingStatus.REJECTED -> colors.statusRechazada
        BookingStatus.EXPIRED -> colors.statusExpirada
        BookingStatus.CANCELLED_BY_PROVIDER -> colors.statusCanceladaProveedor
        BookingStatus.CANCELLED_BY_CUSTOMER -> colors.statusCanceladaCliente
    }
}

/** The wording of each predefined Provider reason (the domain keeps only the codes). */
internal fun ProviderReasonCode.label(): String = when (this) {
    ProviderReasonCode.NOT_AVAILABLE_AT_THIS_TIME -> "No disponible en este horario"
    ProviderReasonCode.OUTSIDE_SERVICE_AREA -> "Fuera de mi zona de servicio"
    ProviderReasonCode.SERVICE_TEMPORARILY_UNAVAILABLE -> "Servicio no disponible temporalmente"
    ProviderReasonCode.OTHER -> "Otro"
}

/** The options `ReasonPicker` lists for rejecting or cancelling; "Otro" alone reveals the note. */
internal val providerReasonOptions: List<ReasonOption> = ProviderReasonCode.entries.map {
    ReasonOption(id = it.name, label = it.label(), allowsNote = it == ProviderReasonCode.OTHER)
}

/** Maps a picker option id back to its code. */
internal fun providerReasonFor(id: String): ProviderReasonCode? =
    ProviderReasonCode.entries.firstOrNull { it.name == id }

/** `Motivo: Otro — texto` as stored on a rejected/cancelled Booking. */
internal fun Reason.text(): String {
    val label = (code as? ProviderReasonCode)?.label() ?: code.storageCode
    return if (note == null) label else "$label — $note"
}
