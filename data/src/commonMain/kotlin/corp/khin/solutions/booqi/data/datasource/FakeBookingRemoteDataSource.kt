package corp.khin.solutions.booqi.data.datasource

import corp.khin.solutions.booqi.data.dto.BookingDraftDto
import corp.khin.solutions.booqi.data.dto.BookingDto

/**
 * TEMPORARY. In-memory stand-in for a real Supabase-backed [BookingRemoteDataSource] until #27
 * (Supabase schema + `supabase-kt` wiring) lands — see `docs/DATABASE.md`. Mirrors the pattern in
 * [FakeServiceRemoteDataSource]: this exists purely so the module graph and MVI wiring can be
 * proven end-to-end without blocking on backend implementation work. Replace, don't extend.
 *
 * Not thread-safe by design — a single fake, single-process instance has no concurrent-writer
 * scenario worth guarding against; a real datasource will get that from the backend instead (this
 * includes the double-booking race: see `BookingRepository.createBooking`).
 */
class FakeBookingRemoteDataSource(
    seed: List<BookingDto> = emptyList(),
) : BookingRemoteDataSource {

    private val bookingsById = mutableMapOf<String, BookingDto>().apply {
        seed.forEach { put(it.id, it) }
    }
    private var nextId = 1

    override suspend fun create(draft: BookingDraftDto): BookingDto {
        val booking = BookingDto(
            id = "booking-${nextId++}",
            providerId = draft.providerId,
            serviceId = draft.serviceId,
            customerId = draft.customerId,
            scheduledAt = draft.scheduledAt,
            durationMinutesSnapshot = draft.durationMinutesSnapshot,
            priceCentsSnapshot = draft.priceCentsSnapshot,
            deliveryAddressLineSnapshot = draft.deliveryAddressLineSnapshot,
            deliveryAddressLatSnapshot = draft.deliveryAddressLatSnapshot,
            deliveryAddressLngSnapshot = draft.deliveryAddressLngSnapshot,
            customerNote = draft.customerNote,
            status = INITIAL_STATUS,
            reasonCode = null,
            reasonNote = null,
            ratingStars = null,
            ratingComment = null,
            requestedAt = draft.requestedAt,
            respondedAt = null,
            completedAt = null,
        )
        bookingsById[booking.id] = booking
        return booking
    }

    override suspend fun findById(bookingId: String): BookingDto? = bookingsById[bookingId]

    override suspend fun findByProviderId(providerId: String): List<BookingDto> =
        bookingsById.values.filter { it.providerId == providerId }

    override suspend fun findByStatus(status: String): List<BookingDto> =
        bookingsById.values.filter { it.status == status }

    override suspend fun save(booking: BookingDto): BookingDto {
        bookingsById[booking.id] = booking
        return booking
    }

    private companion object {
        const val INITIAL_STATUS = "Requested"
    }
}
