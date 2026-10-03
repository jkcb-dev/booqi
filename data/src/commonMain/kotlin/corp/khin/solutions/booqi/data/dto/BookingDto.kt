package corp.khin.solutions.booqi.data.dto

/**
 * Storage shape of one `bookings` row (docs/DATABASE.md). Deliberately not `@Serializable` yet —
 * same reasoning as [ProviderProfileDto]: no real backend contract exists to shape it against
 * (Supabase wiring lands with #27).
 *
 * Primitive/ISO-string representations mirror the column types without presupposing the domain's
 * types — the mapper translates: [scheduledAt] is an ISO local date-time (`"2026-10-05T09:00"`, a
 * timezone-less `timestamp`); [requestedAt]/[respondedAt]/[completedAt] are ISO instants
 * (`"2026-10-03T10:00:00Z"`); [status] is one of `Requested | Confirmed | Completed | Rejected |
 * Expired | CancelledByProvider | CancelledByCustomer`; [reasonCode] is the stored code of a
 * predefined reason. The delivery-address snapshot is three nullable columns (all set, or all
 * `null`).
 */
data class BookingDto(
    val id: String,
    val providerId: String,
    val serviceId: String,
    val customerId: String,
    val scheduledAt: String,
    val durationMinutesSnapshot: Int,
    val priceCentsSnapshot: Int,
    val deliveryAddressLineSnapshot: String?,
    val deliveryAddressLatSnapshot: Double?,
    val deliveryAddressLngSnapshot: Double?,
    val customerNote: String?,
    val status: String,
    val reasonCode: String?,
    val reasonNote: String?,
    val ratingStars: Int?,
    val ratingComment: String?,
    val requestedAt: String,
    val respondedAt: String?,
    val completedAt: String?,
)

/**
 * The fields supplied when creating a [BookingDto] — mirrors the domain's
 * [corp.khin.solutions.booqi.domain.model.BookingDraft]. The id and the initial `"Requested"`
 * status are assigned by the datasource (as a real backend would on insert).
 */
data class BookingDraftDto(
    val providerId: String,
    val serviceId: String,
    val customerId: String,
    val scheduledAt: String,
    val durationMinutesSnapshot: Int,
    val priceCentsSnapshot: Int,
    val deliveryAddressLineSnapshot: String?,
    val deliveryAddressLatSnapshot: Double?,
    val deliveryAddressLngSnapshot: Double?,
    val customerNote: String?,
    val requestedAt: String,
)
