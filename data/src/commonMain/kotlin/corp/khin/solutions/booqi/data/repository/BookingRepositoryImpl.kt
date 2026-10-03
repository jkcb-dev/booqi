package corp.khin.solutions.booqi.data.repository

import corp.khin.solutions.booqi.core.common.DomainError
import corp.khin.solutions.booqi.core.common.DomainResult
import corp.khin.solutions.booqi.core.common.asFailure
import corp.khin.solutions.booqi.core.common.asSuccess
import corp.khin.solutions.booqi.data.datasource.BookingRemoteDataSource
import corp.khin.solutions.booqi.data.mapper.toDomain
import corp.khin.solutions.booqi.data.mapper.toDto
import corp.khin.solutions.booqi.domain.model.Booking
import corp.khin.solutions.booqi.domain.model.BookingDraft
import corp.khin.solutions.booqi.domain.model.BookingStatus
import corp.khin.solutions.booqi.domain.repository.BookingRepository
import kotlinx.datetime.Instant

/**
 * Maps `bookings` rows to [Booking]s, does the status/rating/cutoff filtering and enforces the
 * ordering contracts documented on [BookingRepository], so a real datasource can return rows in
 * any order.
 */
class BookingRepositoryImpl(
    private val remoteDataSource: BookingRemoteDataSource,
) : BookingRepository {

    // Deliberate: this boundary is where every real exception (network, serialization, a
    // malformed stored value, ...) gets translated into a DomainError — see core:common's
    // DomainResult docs. Same pattern as ServiceRepositoryImpl.
    @Suppress("TooGenericExceptionCaught")
    override suspend fun createBooking(draft: BookingDraft): DomainResult<Booking> = try {
        remoteDataSource.create(draft.toDto()).toDomain().asSuccess()
    } catch (e: Exception) {
        DomainError.Unknown(e.message).asFailure()
    }

    @Suppress("TooGenericExceptionCaught")
    override suspend fun getBooking(bookingId: String): DomainResult<Booking> = try {
        remoteDataSource.findById(bookingId)?.toDomain()?.asSuccess()
            ?: DomainError.NotFound.asFailure()
    } catch (e: Exception) {
        DomainError.Unknown(e.message).asFailure()
    }

    @Suppress("TooGenericExceptionCaught")
    override suspend fun updateBooking(booking: Booking): DomainResult<Booking> = try {
        if (remoteDataSource.findById(booking.id) == null) {
            DomainError.NotFound.asFailure()
        } else {
            remoteDataSource.save(booking.toDto()).toDomain().asSuccess()
        }
    } catch (e: Exception) {
        DomainError.Unknown(e.message).asFailure()
    }

    @Suppress("TooGenericExceptionCaught")
    override suspend fun getBookingsByProvider(
        providerId: String,
        statuses: Set<BookingStatus>?,
    ): DomainResult<List<Booking>> = try {
        remoteDataSource.findByProviderId(providerId)
            .map { it.toDomain() }
            .filter { statuses == null || it.status in statuses }
            .sortedWith(compareBy({ it.scheduledAt }, { it.id }))
            .asSuccess()
    } catch (e: Exception) {
        DomainError.Unknown(e.message).asFailure()
    }

    @Suppress("TooGenericExceptionCaught")
    override suspend fun getPendingRequestedAtOrBefore(cutoff: Instant): DomainResult<List<Booking>> = try {
        remoteDataSource.findByStatus(PENDING)
            .map { it.toDomain() }
            .filter { it.requestedAt <= cutoff }
            .sortedWith(compareBy({ it.requestedAt }, { it.id }))
            .asSuccess()
    } catch (e: Exception) {
        DomainError.Unknown(e.message).asFailure()
    }

    // completedAt is null only for a (data-inconsistent) rated booking that never completed; it
    // sorts last under descending order, which is the harmless place for it.
    @Suppress("TooGenericExceptionCaught")
    override suspend fun getRatedBookingsByProvider(providerId: String): DomainResult<List<Booking>> = try {
        remoteDataSource.findByProviderId(providerId)
            .map { it.toDomain() }
            .filter { it.rating != null }
            .sortedWith(
                compareByDescending<Booking> { it.completedAt }
                    .thenByDescending { it.scheduledAt }
                    .thenBy { it.id },
            )
            .asSuccess()
    } catch (e: Exception) {
        DomainError.Unknown(e.message).asFailure()
    }

    private companion object {
        const val PENDING = "Requested"
    }
}
