package corp.khin.solutions.booqi.feature.booking

import corp.khin.solutions.booqi.core.common.DomainError
import corp.khin.solutions.booqi.domain.model.Booking
import corp.khin.solutions.booqi.domain.model.BookingStatus
import corp.khin.solutions.booqi.domain.model.CustomerReasonCode
import kotlinx.datetime.TimeZone
import kotlin.time.Instant

/** Which Figma screen a Booking is shown as (C8-C10b, plus the closed ones that have none). */
enum class BookingDetailVariant { PENDING, CONFIRMED, REJECTED, EXPIRED, COMPLETED, CANCELLED }

/** The open "Cancelar cita" reason picker (the CUSTOMER reasons). [error] is the missing-reason message. */
data class CancelFormState(
    val code: CustomerReasonCode? = null,
    val note: String = "",
    val error: String? = null,
)

/**
 * Immutable state of the Booking detail (C8 pending, C9 confirmed, C10 rejected, C10b expired, and
 * the completed / cancelled ones). [bookingId] is the Booking this state was loaded *for* (the
 * ViewModel instance is reused across visits). [now] and [timeZone] come from the ViewModel's
 * injected clock and drive the countdowns and the 3h rule, so they are testable.
 */
data class BookingDetailUiState(
    val now: Instant,
    val timeZone: TimeZone,
    val bookingId: String? = null,
    val isLoading: Boolean = true,
    val booking: Booking? = null,
    val serviceTitle: String? = null,
    val providerName: String? = null,
    val error: DomainError? = null,
    val cancelForm: CancelFormState? = null,
    val isCancelling: Boolean = false,
    val actionError: String? = null,
    val notice: String? = null,
) {
    /**
     * The screen variant of the Booking. A REQUESTED one whose 24h window has lapsed is shown as
     * expired even before the backend's sweep stores it (docs/ARCHITECTURE.md § Booking expiry,
     * option C: lazy, on read), so the Customer never sees a "Pendiente" that can no longer be
     * answered.
     */
    val variant: BookingDetailVariant?
        get() = booking?.let { b ->
            when (b.effectiveStatus(now)) {
                BookingStatus.REQUESTED -> BookingDetailVariant.PENDING
                BookingStatus.CONFIRMED -> BookingDetailVariant.CONFIRMED
                BookingStatus.REJECTED -> BookingDetailVariant.REJECTED
                BookingStatus.EXPIRED -> BookingDetailVariant.EXPIRED
                BookingStatus.COMPLETED -> BookingDetailVariant.COMPLETED
                BookingStatus.CANCELLED_BY_CUSTOMER, BookingStatus.CANCELLED_BY_PROVIDER ->
                    BookingDetailVariant.CANCELLED
            }
        }

    /** C9: "Cancelar cita" is offered only until 3h before the appointment (inclusive). */
    val canCancel: Boolean
        get() = booking?.canBeCancelledByCustomer(now, timeZone) == true

    /** The textual countdown: C8's time left for the Provider to answer, C9's time to the appointment. */
    val countdown: String?
        get() = booking?.let { b ->
            when (variant) {
                BookingDetailVariant.PENDING -> countdownText(b.timeToRespond(now))
                BookingDetailVariant.CONFIRMED -> {
                    val left = b.timeUntilStart(now, timeZone)
                    if (left.isPositive()) countdownText(left) else null
                }
                else -> null
            }
        }

    /** True once a COMPLETED Booking can still be rated (C12 is reachable from the detail). */
    val canRate: Boolean
        get() = booking?.let { it.status == BookingStatus.COMPLETED && it.rating == null } == true
}

/** The status the Customer should see at [now]: REQUESTED past its 24h window reads as EXPIRED. */
internal fun Booking.effectiveStatus(now: Instant): BookingStatus =
    if (isResponseOverdue(now)) BookingStatus.EXPIRED else status
