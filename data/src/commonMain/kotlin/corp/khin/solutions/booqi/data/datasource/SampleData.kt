package corp.khin.solutions.booqi.data.datasource

import corp.khin.solutions.booqi.data.dto.BookingDto
import corp.khin.solutions.booqi.data.dto.ProviderProfileDto
import corp.khin.solutions.booqi.data.dto.ServiceDto
import corp.khin.solutions.booqi.data.dto.UserDto
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
 * TEMPORARY sample data seeded into the in-memory fakes so the app has something to show on a device
 * before the Customer booking UI (#26) and Supabase (#27) exist. Goes away with the fakes.
 * Dates are relative to [now] so "future"/"past" appointments stay meaningful whenever the app runs.
 *
 * **Ids follow the contract settled in #56:** `Service`/`Booking`.`providerId` is a
 * *ProviderProfile id*, and a profile points at its owner through `userId` — a seeded [accounts]
 * User. So every sample provider below is a profile with an owner:
 * - the two catalog profiles ([SAMPLE_PROFILE_ID], [SAMPLE_PROFILE_2_ID]), owned by sample Users
 *   nobody can sign in to, so the Customer's search (#20/#21) has data with coordinates,
 *   categories and ratings;
 * - the **demo Provider** ([DEMO_PROVIDER_PROFILE_ID], owned by the demo account), which holds the
 *   two provider-side services, the sample bookings and their ratings — what the Provider screens
 *   (Servicios, Solicitudes, Horario, Calificaciones) show after signing in as the demo account.
 *   Its profile is complete, so its services also appear in the Customer's search, like any service
 *   a Provider creates in the app.
 *
 * **Demo account (fake-only test credentials — this app has no real accounts until Supabase Auth,
 * #27):** email `demo@booqi.test`, password `Demo1234`. It is verified, so it can book and activate
 * Provider mode, and it already owns the demo Provider profile above. Its active bookings as
 * Provider also make "Eliminar cuenta" answer with the blocking list. To try the unverified flow or
 * a deletable account, sign up with any other email (a new email account starts unverified).
 *
 * The sample Customer ([SAMPLE_CUSTOMER_ID], "Camila Torres") made the bookings; one completed
 * booking belongs to a deleted account ([DELETED_CUSTOMER_ID]) so "Usuario eliminado" shows too.
 */
object SampleData {

    const val DEMO_EMAIL = "demo@booqi.test"
    const val DEMO_PASSWORD = "Demo1234"
    const val DEMO_USER_ID = "sample-user-demo"
    const val DEMO_PROVIDER_PROFILE_ID = "sample-provider-profile-demo"

    const val SAMPLE_CUSTOMER_ID = "sample-customer"
    const val DELETED_CUSTOMER_ID = "sample-customer-deleted"

    const val SAMPLE_PROFILE_ID = "sample-provider-profile-1"
    const val SAMPLE_PROFILE_2_ID = "sample-provider-profile-2"

    private const val GEL_ID = "sample-service-gel"
    private const val CUT_ID = "sample-service-cut"

    /** The seeded accounts: the demo account, the sample Customers and the profiles' owners. */
    fun accounts(): List<SeedAccount> = sampleAccounts()

    /**
     * Complete, coordinate-bearing profiles for the Catalog: two with their own owners, and the demo
     * Provider's. Activating Provider mode as any *other* user creates a fresh, incomplete profile.
     */
    fun providerProfiles(): List<ProviderProfileDto> = listOf(
        sampleProfile(
            ProfileSeed(
                id = SAMPLE_PROFILE_ID,
                name = "Studio Booqi",
                location = "Av. Corrientes 1234, CABA",
                coordinates = -34.6037 to -58.3816,
                rating = 4.5 to 2,
            ),
        ),
        sampleProfile(
            ProfileSeed(
                id = SAMPLE_PROFILE_2_ID,
                name = "Casa Brillante",
                location = "Av. Santa Fe 3200, Palermo, CABA",
                coordinates = -34.5880 to -58.4100,
                rating = 4.8 to 12,
            ),
        ),
        sampleProfile(
            ProfileSeed(
                id = DEMO_PROVIDER_PROFILE_ID,
                name = "Demo Studio",
                location = "Av. Callao 900, CABA",
                coordinates = -34.5990 to -58.3930,
                rating = 4.5 to 2,
                userId = DEMO_USER_ID,
            ),
        ),
    )

    fun services(): List<ServiceDto> = providerSideServices() + catalogServices()

    private fun providerSideServices(): List<ServiceDto> = listOf(
        ServiceDto(
            id = GEL_ID,
            providerId = DEMO_PROVIDER_PROFILE_ID,
            title = "Manicure gel",
            photoUrl = "https://example.test/gel.jpg",
            description = "Manicure con esmaltado en gel.",
            priceCents = 2550,
            durationMinutes = 45,
            modality = "local",
            isActive = true,
            category = "unas",
        ),
        ServiceDto(
            id = CUT_ID,
            providerId = DEMO_PROVIDER_PROFILE_ID,
            title = "Corte a domicilio",
            photoUrl = "https://example.test/cut.jpg",
            description = "Corte de cabello en tu casa.",
            priceCents = 3000,
            durationMinutes = 60,
            modality = "domicilio",
            isActive = true,
            category = "barberia",
        ),
    )

    private fun catalogServices(): List<ServiceDto> = listOf(
        catalogService("sample-catalog-1", SAMPLE_PROFILE_ID, "Corte y barba", "barberia", "local"),
        catalogService("sample-catalog-2", SAMPLE_PROFILE_ID, "Masaje descontracturante", "masajes", "local"),
        catalogService("sample-catalog-3", SAMPLE_PROFILE_2_ID, "Limpieza de hogar", "limpieza", "domicilio"),
        catalogService("sample-catalog-4", SAMPLE_PROFILE_2_ID, "Reparación de heladeras", "tecnico", "domicilio"),
        catalogService("sample-catalog-5", SAMPLE_PROFILE_2_ID, "Uñas esculpidas", "unas", "ambos"),
    )

    private fun catalogService(
        id: String,
        providerId: String,
        title: String,
        category: String,
        modality: String,
    ) = ServiceDto(
        id = id,
        providerId = providerId,
        title = title,
        photoUrl = "https://example.test/$id.jpg",
        description = "$title, a cargo de un profesional de confianza.",
        priceCents = 4000,
        durationMinutes = 60,
        modality = modality,
        isActive = true,
        category = category,
    )

    private fun sampleProfile(seed: ProfileSeed) = ProviderProfileDto(
        id = seed.id,
        userId = seed.userId,
        name = seed.name,
        photoUrl = "https://example.test/${seed.id}.jpg",
        description = "${seed.name}: atención profesional con reserva previa.",
        location = seed.location,
        isComplete = true,
        pausedRangeStart = null,
        pausedRangeEnd = null,
        ratingAverage = seed.rating.first,
        ratingCount = seed.rating.second,
        locationLat = seed.coordinates.first,
        locationLng = seed.coordinates.second,
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
                    customerId = DELETED_CUSTOMER_ID,
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
            providerId = DEMO_PROVIDER_PROFILE_ID,
            serviceId = serviceId,
            customerId = SAMPLE_CUSTOMER_ID,
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
