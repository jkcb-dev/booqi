package corp.khin.solutions.booqi.data.repository

import corp.khin.solutions.booqi.core.common.DomainError
import corp.khin.solutions.booqi.core.common.DomainResult
import corp.khin.solutions.booqi.data.datasource.FakeBookingRemoteDataSource
import corp.khin.solutions.booqi.data.datasource.FakeProviderProfileRemoteDataSource
import corp.khin.solutions.booqi.data.dto.BookingDraftDto
import corp.khin.solutions.booqi.domain.model.BookingStatus
import corp.khin.solutions.booqi.domain.model.ProviderProfile
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** The repository methods added for Identity (#56): the user → profile path, anonymizing a profile, bookings by customer. */
class IdentityDataTest {

    private val profileSource = FakeProviderProfileRemoteDataSource()
    private val profiles = ProviderProfileRepositoryImpl(profileSource)
    private val bookingSource = FakeBookingRemoteDataSource()
    private val bookings = BookingRepositoryImpl(bookingSource)

    private fun <T> DomainResult<T>.ok(): T = (this as DomainResult.Success<T>).value

    @Test
    fun `findByUserId returns the profile of that user or null`() = runTest {
        assertNull(profiles.findByUserId("user-1").ok())
        val created = profiles.activateProviderMode("user-1").ok()

        assertEquals(created, profiles.findByUserId("user-1").ok())
        assertNull(profiles.findByUserId("user-2").ok())
    }

    @Test
    fun `anonymizing a profile erases personal data hides it and keeps the rating summary`() = runTest {
        val created = profiles.activateProviderMode("user-1").ok()
        profiles.completeProfile(created.id, "Studio Ana", "https://example.com/p.jpg", "Manicuría", "Av. Santa Fe 1")
        profiles.updateRating(created.id, 4.5, 2)

        val anonymized = profiles.anonymizeProfile(created.id).ok()

        assertEquals(
            ProviderProfile(id = created.id, userId = "user-1", ratingAverage = 4.5, ratingCount = 2),
            anonymized,
        )
        assertEquals(anonymized, profiles.anonymizeProfile(created.id).ok()) // idempotent
        assertEquals(DomainError.NotFound, (profiles.anonymizeProfile("missing") as DomainResult.Failure).error)
    }

    private suspend fun requestBooking(customerId: String, scheduledAt: String) = bookingSource.create(
        BookingDraftDto(
            providerId = "p1",
            serviceId = "s1",
            customerId = customerId,
            scheduledAt = scheduledAt,
            durationMinutesSnapshot = 60,
            priceCentsSnapshot = 3500,
            deliveryAddressLineSnapshot = null,
            deliveryAddressLatSnapshot = null,
            deliveryAddressLngSnapshot = null,
            customerNote = null,
            requestedAt = "2026-10-05T08:00:00Z",
        ),
    )

    @Test
    fun `getBookingsByCustomer filters by customer and status and orders by date`() = runTest {
        requestBooking("c1", "2026-10-13T10:00")
        val early = requestBooking("c1", "2026-10-12T10:00")
        requestBooking("c2", "2026-10-11T10:00")
        bookingSource.save(early.copy(status = "Confirmed"))

        assertEquals(listOf("booking-2", "booking-1"), bookings.getBookingsByCustomer("c1").ok().map { it.id })
        assertEquals(
            listOf("booking-2"),
            bookings.getBookingsByCustomer("c1", setOf(BookingStatus.CONFIRMED)).ok().map { it.id },
        )
        assertEquals(emptyList(), bookings.getBookingsByCustomer("nobody").ok())
    }
}
