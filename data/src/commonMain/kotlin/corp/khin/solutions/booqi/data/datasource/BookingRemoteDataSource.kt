package corp.khin.solutions.booqi.data.datasource

import corp.khin.solutions.booqi.data.dto.BookingDraftDto
import corp.khin.solutions.booqi.data.dto.BookingDto

/**
 * Source of `Booking` data (`bookings` table, docs/DATABASE.md) — create, then read/mutate by id.
 * `data`-only concern; the DTO shape never crosses into `domain`
 * ([corp.khin.solutions.booqi.data.mapper] does that translation). Ordering is not part of this
 * contract — the repository normalizes it, and also does the status/rating/cutoff filtering, so a
 * real datasource may later push those into queries without changing callers.
 */
interface BookingRemoteDataSource {

    /** Creates and persists a new booking from [draft], with status `"Requested"`. Id assignment
     * is this datasource's responsibility (mirrors [ServiceRemoteDataSource.create]). */
    suspend fun create(draft: BookingDraftDto): BookingDto

    suspend fun findById(bookingId: String): BookingDto?

    /** Every booking of [providerId], in any status; empty if none. */
    suspend fun findByProviderId(providerId: String): List<BookingDto>

    /** Every booking, across providers, whose status is exactly [status]; empty if none. */
    suspend fun findByStatus(status: String): List<BookingDto>

    /** Upserts by [BookingDto.id]. */
    suspend fun save(booking: BookingDto): BookingDto
}
