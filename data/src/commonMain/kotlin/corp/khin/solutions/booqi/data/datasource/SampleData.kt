package corp.khin.solutions.booqi.data.datasource

import corp.khin.solutions.booqi.data.dto.BookingDto
import corp.khin.solutions.booqi.data.dto.ServiceDto
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant

/**
 * TEMPORARY sample data seeded into the in-memory fakes so the Provider's booking inbox (#19) has
 * something to show on a device: there is no Customer UI yet that could create bookings. Goes away
 * with the fakes when #27 (Supabase) lands.
 *
 * [PROVIDER_ID] must match `feature:provider`'s `TEMPORARY_PROVIDER_ID` (a user id — whether
 * `Booking.providerId` should be a user id or a ProviderProfile id is open in #50). Dates are
 * relative to [now] so "future"/"past" appointments stay meaningful whenever the app runs.
 */
object SampleData {

    const val PROVIDER_ID = "user-placeholder-temp"

    private const val GEL_ID = "sample-service-gel"
    private const val CUT_ID = "sample-service-cut"

    fun services(): List<ServiceDto> = listOf(
        ServiceDto(
            id = GEL_ID,
            providerId = PROVIDER_ID,
            title = "Manicure gel",
            photoUrl = "https://example.test/gel.jpg",
            description = "Manicure con esmaltado en gel.",
            priceCents = 2550,
            durationMinutes = 45,
            modality = "local",
            isActive = true,
        ),
        ServiceDto(
            id = CUT_ID,
            providerId = PROVIDER_ID,
            title = "Corte a domicilio",
            photoUrl = "https://example.test/cut.jpg",
            description = "Corte de cabello en tu casa.",
            priceCents = 3000,
            durationMinutes = 60,
            modality = "domicilio",
            isActive = true,
        ),
    )

    fun bookings(
        now: Instant = Clock.System.now(),
        timeZone: TimeZone = TimeZone.currentSystemDefault(),
    ): List<BookingDto> {
        val today = now.toLocalDateTime(timeZone).date
        fun at(daysFromToday: Int, hour: Int) =
            LocalDateTime(today.plus(daysFromToday, DateTimeUnit.DAY), LocalTime(hour, 0)).toString()

        return listOf(
            booking("sample-booking-requested", GEL_ID, at(daysFromToday = 1, hour = 10), now - 2.hours)
                .copy(customerNote = "¿Puede ser con diseño francés?"),
            booking("sample-booking-confirmed-future", CUT_ID, at(daysFromToday = 2, hour = 15), now - 1.days)
                .copy(status = "Confirmed", respondedAt = (now - 20.hours).toString()),
            booking("sample-booking-confirmed-past", GEL_ID, at(daysFromToday = -1, hour = 9), now - 3.days)
                .copy(status = "Confirmed", respondedAt = (now - 3.days + 1.hours).toString()),
            booking("sample-booking-completed-1", GEL_ID, at(daysFromToday = -7, hour = 11), now - 9.days)
                .copy(
                    status = "Completed",
                    respondedAt = (now - 9.days + 2.hours).toString(),
                    completedAt = (now - 7.days).toString(),
                    ratingStars = 5,
                    ratingComment = "Excelente, muy prolija y puntual.",
                ),
            booking("sample-booking-completed-2", CUT_ID, at(daysFromToday = -14, hour = 16), now - 16.days)
                .copy(
                    status = "Completed",
                    respondedAt = (now - 16.days + 1.hours).toString(),
                    completedAt = (now - 14.days).toString(),
                    ratingStars = 4,
                    ratingComment = "Muy buen corte, llegó un poco tarde.",
                ),
        )
    }

    /** A Requested booking for [serviceId]; callers `copy()` it into later states. */
    private fun booking(
        id: String,
        serviceId: String,
        scheduledAt: String,
        requestedAt: Instant,
    ): BookingDto {
        val service = services().first { it.id == serviceId }
        // Domicilio bookings snapshot the Customer's address at request time (docs/DOMAIN.md).
        val atCustomer = service.modality == "domicilio"
        return BookingDto(
            id = id,
            providerId = PROVIDER_ID,
            serviceId = serviceId,
            customerId = "sample-customer",
            scheduledAt = scheduledAt,
            durationMinutesSnapshot = service.durationMinutes,
            priceCentsSnapshot = service.priceCents,
            deliveryAddressLineSnapshot = if (atCustomer) "Av. Corrientes 1234, CABA" else null,
            deliveryAddressLatSnapshot = if (atCustomer) -34.6037 else null,
            deliveryAddressLngSnapshot = if (atCustomer) -58.3816 else null,
            customerNote = null,
            status = "Requested",
            reasonCode = null,
            reasonNote = null,
            ratingStars = null,
            ratingComment = null,
            requestedAt = requestedAt.toString(),
            respondedAt = null,
            completedAt = null,
        )
    }
}
