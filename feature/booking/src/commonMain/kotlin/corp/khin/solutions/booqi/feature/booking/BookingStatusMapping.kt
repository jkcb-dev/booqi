package corp.khin.solutions.booqi.feature.booking

import corp.khin.solutions.booqi.core.designsystem.component.StatusBadgeStyle
import corp.khin.solutions.booqi.domain.model.BookingStatus

/** The Customer's wording of each [BookingStatus] on the badge. */
internal fun BookingStatus.customerLabel(): String = when (this) {
    BookingStatus.REQUESTED -> "Pendiente"
    BookingStatus.CONFIRMED -> "Confirmada"
    BookingStatus.COMPLETED -> "Completada"
    BookingStatus.REJECTED -> "Rechazada"
    BookingStatus.EXPIRED -> "Expirada"
    BookingStatus.CANCELLED_BY_PROVIDER -> "Cancelada por el proveedor"
    BookingStatus.CANCELLED_BY_CUSTOMER -> "Cancelada por ti"
}

/** Maps a [BookingStatus] onto the design system's status look. */
internal fun BookingStatus.badgeStyle(): StatusBadgeStyle = when (this) {
    BookingStatus.REQUESTED -> StatusBadgeStyle.PENDIENTE
    BookingStatus.CONFIRMED -> StatusBadgeStyle.CONFIRMADA
    BookingStatus.COMPLETED -> StatusBadgeStyle.COMPLETADA
    BookingStatus.REJECTED -> StatusBadgeStyle.RECHAZADA
    BookingStatus.EXPIRED -> StatusBadgeStyle.EXPIRADA
    BookingStatus.CANCELLED_BY_PROVIDER -> StatusBadgeStyle.CANCELADA_PROVEEDOR
    BookingStatus.CANCELLED_BY_CUSTOMER -> StatusBadgeStyle.CANCELADA_CLIENTE
}
