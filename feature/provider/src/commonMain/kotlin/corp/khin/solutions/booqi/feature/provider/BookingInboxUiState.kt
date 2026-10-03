package corp.khin.solutions.booqi.feature.provider

import corp.khin.solutions.booqi.core.common.DomainError
import corp.khin.solutions.booqi.domain.model.Booking
import corp.khin.solutions.booqi.domain.model.ProviderReasonCode
import kotlin.time.Instant
import kotlinx.datetime.TimeZone

/** The two lists of the screen: "Solicitudes" (Figma P8) and "Confirmadas" (the P10 list). */
enum class BookingInboxTab { REQUESTS, CONFIRMED }

/** Which of the two reason flows is open: rejecting a request (P9) or cancelling an accepted
 * appointment (P10). Both use the same Provider reason list. */
enum class ReasonIntent { REJECT, CANCEL }

/**
 * Immutable state of the whole `BookingRequestInbox` screen: the two lists plus, when the
 * Provider opened a row, the in-screen [detail] (P9 / P10) that replaces them.
 *
 * [serviceTitles] maps `Booking.serviceId` to its title (a Booking only references its Service);
 * a missing entry renders as a generic "Servicio". [now]/[timeZone] drive the "Completar only
 * after the appointment ends" rule — [now] is refreshed on every load and on a periodic
 * [BookingInboxAction.Tick], so the button enables itself while the Provider is looking at it.
 */
data class BookingInboxUiState(
    val now: Instant,
    val timeZone: TimeZone = TimeZone.currentSystemDefault(),
    val tab: BookingInboxTab = BookingInboxTab.REQUESTS,
    // Starts true so the first frame before the screen's Start never flashes the empty state.
    val isLoading: Boolean = true,
    val requests: List<Booking> = emptyList(),
    val confirmed: List<Booking> = emptyList(),
    val serviceTitles: Map<String, String> = emptyMap(),
    val loadError: DomainError? = null,
    /** Result of the last accept/reject/complete/cancel ("Solicitud aceptada"), shown over the list. */
    val notice: BookingNotice? = null,
    /** Id of the booking whose transition is in flight (buttons disabled meanwhile), if any. */
    val submittingBookingId: String? = null,
    val detail: BookingDetailUiState? = null,
)

/** A message over the lists: the outcome of an action, or why a row action failed. */
data class BookingNotice(val message: String, val isError: Boolean = false)

/**
 * The opened booking (P9 for a request, P10 for a confirmed appointment). [booking] starts as the
 * row the Provider tapped and is replaced by the fresh copy `ObtenerReserva` returns, so a stale
 * row never drives an action. [actionError] is the inline message for any failed action
 * (`InvalidInput` for an invalid transition or a lapsed request, never a crash).
 */
data class BookingDetailUiState(
    val booking: Booking,
    val actionError: String? = null,
    val reasonForm: ReasonFormState? = null,
)

/** The open reject/cancel reason picker: the chosen [code] (none yet = confirm is blocked) and the
 * optional free-text [note] that only "Otro" uses. */
data class ReasonFormState(
    val intent: ReasonIntent,
    val code: ProviderReasonCode? = null,
    val note: String = "",
    val error: String? = null,
)
