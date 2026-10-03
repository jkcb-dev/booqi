package corp.khin.solutions.booqi.data.mapper

import corp.khin.solutions.booqi.data.dto.BookingDraftDto
import corp.khin.solutions.booqi.data.dto.BookingDto
import corp.khin.solutions.booqi.domain.model.Address
import corp.khin.solutions.booqi.domain.model.Booking
import corp.khin.solutions.booqi.domain.model.BookingDraft
import corp.khin.solutions.booqi.domain.model.BookingStatus
import corp.khin.solutions.booqi.domain.model.ProviderReasonCode
import corp.khin.solutions.booqi.domain.model.Rating
import corp.khin.solutions.booqi.domain.model.Reason
import corp.khin.solutions.booqi.domain.model.ReasonCode
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime

fun BookingDto.toDomain(): Booking = Booking(
    id = id,
    providerId = providerId,
    serviceId = serviceId,
    customerId = customerId,
    scheduledAt = LocalDateTime.parse(scheduledAt),
    durationMinutesSnapshot = durationMinutesSnapshot,
    priceCentsSnapshot = priceCentsSnapshot,
    requestedAt = Instant.parse(requestedAt),
    status = status.toDomainStatus(),
    deliveryAddress = toDomainAddress(),
    customerNote = customerNote,
    reason = reasonCode?.let { Reason(it.toDomainReasonCode(), reasonNote) },
    rating = ratingStars?.let { Rating(it, ratingComment) },
    respondedAt = respondedAt?.let(Instant::parse),
    completedAt = completedAt?.let(Instant::parse),
)

// The three address columns are all set or all null; any other combination is a data
// inconsistency the mapper doesn't try to guess around (same stance as ProviderProfileMapper).
private fun BookingDto.toDomainAddress(): Address? {
    val line = deliveryAddressLineSnapshot
    val lat = deliveryAddressLatSnapshot
    val lng = deliveryAddressLngSnapshot
    return if (line != null && lat != null && lng != null) Address(line, lat, lng) else null
}

fun Booking.toDto(): BookingDto = BookingDto(
    id = id,
    providerId = providerId,
    serviceId = serviceId,
    customerId = customerId,
    scheduledAt = scheduledAt.toString(),
    durationMinutesSnapshot = durationMinutesSnapshot,
    priceCentsSnapshot = priceCentsSnapshot,
    deliveryAddressLineSnapshot = deliveryAddress?.line,
    deliveryAddressLatSnapshot = deliveryAddress?.latitude,
    deliveryAddressLngSnapshot = deliveryAddress?.longitude,
    customerNote = customerNote,
    status = status.toDtoStatus(),
    reasonCode = reason?.code?.storageCode,
    reasonNote = reason?.note,
    ratingStars = rating?.stars,
    ratingComment = rating?.comment,
    requestedAt = requestedAt.toString(),
    respondedAt = respondedAt?.toString(),
    completedAt = completedAt?.toString(),
)

fun BookingDraft.toDto(): BookingDraftDto = BookingDraftDto(
    providerId = providerId,
    serviceId = serviceId,
    customerId = customerId,
    scheduledAt = scheduledAt.toString(),
    durationMinutesSnapshot = durationMinutesSnapshot,
    priceCentsSnapshot = priceCentsSnapshot,
    deliveryAddressLineSnapshot = deliveryAddress?.line,
    deliveryAddressLatSnapshot = deliveryAddress?.latitude,
    deliveryAddressLngSnapshot = deliveryAddress?.longitude,
    customerNote = customerNote,
    requestedAt = requestedAt.toString(),
)

// Explicit both ways (not enum names) so the stored strings stay exactly the ones in
// docs/DATABASE.md even if a Kotlin entry is renamed.
private fun String.toDomainStatus(): BookingStatus = when (this) {
    "Requested" -> BookingStatus.REQUESTED
    "Confirmed" -> BookingStatus.CONFIRMED
    "Completed" -> BookingStatus.COMPLETED
    "Rejected" -> BookingStatus.REJECTED
    "Expired" -> BookingStatus.EXPIRED
    "CancelledByProvider" -> BookingStatus.CANCELLED_BY_PROVIDER
    "CancelledByCustomer" -> BookingStatus.CANCELLED_BY_CUSTOMER
    else -> error("Unknown Booking status: $this")
}

private fun BookingStatus.toDtoStatus(): String = when (this) {
    BookingStatus.REQUESTED -> "Requested"
    BookingStatus.CONFIRMED -> "Confirmed"
    BookingStatus.COMPLETED -> "Completed"
    BookingStatus.REJECTED -> "Rejected"
    BookingStatus.EXPIRED -> "Expired"
    BookingStatus.CANCELLED_BY_PROVIDER -> "CancelledByProvider"
    BookingStatus.CANCELLED_BY_CUSTOMER -> "CancelledByCustomer"
}

// Storage codes are unique across every ReasonCode implementation, so one lookup finds the enum;
// ticket #25 adds its CustomerReasonCode.entries here.
private fun String.toDomainReasonCode(): ReasonCode =
    ProviderReasonCode.entries.firstOrNull { it.storageCode == this }
        ?: error("Unknown Booking reason code: $this")
