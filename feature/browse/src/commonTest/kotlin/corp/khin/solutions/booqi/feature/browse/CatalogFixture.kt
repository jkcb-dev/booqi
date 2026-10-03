package corp.khin.solutions.booqi.feature.browse

import corp.khin.solutions.booqi.domain.model.Booking
import corp.khin.solutions.booqi.domain.model.BookingStatus
import corp.khin.solutions.booqi.domain.model.CatalogEntry
import corp.khin.solutions.booqi.domain.model.GeoPoint
import corp.khin.solutions.booqi.domain.model.ProviderProfile
import corp.khin.solutions.booqi.domain.model.Rating
import corp.khin.solutions.booqi.domain.model.Service
import corp.khin.solutions.booqi.domain.model.ServiceCategory
import corp.khin.solutions.booqi.domain.model.ServiceModality
import kotlinx.datetime.LocalDateTime
import kotlin.time.Clock
import kotlin.time.Instant

/** A fixed "now" so the use case's "not paused today" check is deterministic. */
object FixedClock : Clock {
    override fun now(): Instant = Instant.parse("2026-10-03T12:00:00Z")
}

/**
 * The same shape as `SampleData`'s catalog (two complete profiles with coordinates, categories
 * and ratings) plus a "Corte de uñas", so "corte" matches two categories. Studio Booqi is ~1.3 km
 * from the TEMPORARY customer location and Casa Brillante ~4.4 km.
 */
object CatalogFixture {
    const val STUDIO_ID = "p1"
    const val CASA_ID = "p2"

    val studio = ProviderProfile(
        id = STUDIO_ID,
        userId = "u1",
        name = "Studio Booqi",
        photoUrl = "https://example.test/p1.jpg",
        description = "Atención profesional con reserva previa.",
        location = "Av. Corrientes 1234, CABA",
        isComplete = true,
        ratingAverage = 4.5,
        ratingCount = 2,
        coordinates = GeoPoint(latitude = -34.6037, longitude = -58.3816),
    )

    val casa = ProviderProfile(
        id = CASA_ID,
        userId = "u2",
        name = "Casa Brillante",
        photoUrl = "https://example.test/p2.jpg",
        description = "Limpieza y arreglos en tu casa.",
        location = "Av. Santa Fe 3200, Palermo, CABA",
        isComplete = true,
        ratingAverage = 4.8,
        ratingCount = 12,
        coordinates = GeoPoint(latitude = -34.5880, longitude = -58.4100),
    )

    fun service(
        id: String,
        providerId: String,
        title: String,
        category: ServiceCategory,
        modality: ServiceModality = ServiceModality.LOCAL,
        isActive: Boolean = true,
    ) = Service(
        id = id,
        providerId = providerId,
        title = title,
        photoUrl = "https://example.test/$id.jpg",
        description = "$title, a cargo de un profesional de confianza.",
        priceCents = 4000,
        durationMinutes = 60,
        modality = modality,
        isActive = isActive,
        category = category,
    )

    val corteYBarba = service("s1", STUDIO_ID, "Corte y barba", ServiceCategory.BARBERIA)
    val masaje = service("s2", STUDIO_ID, "Masaje descontracturante", ServiceCategory.MASAJES)
    val corteDeUnas = service("s6", STUDIO_ID, "Corte de uñas", ServiceCategory.UNAS)
    val limpieza = service("s3", CASA_ID, "Limpieza de hogar", ServiceCategory.LIMPIEZA, ServiceModality.DOMICILIO)
    val heladeras = service("s4", CASA_ID, "Reparación de heladeras", ServiceCategory.TECNICO, ServiceModality.DOMICILIO)
    val unasEsculpidas = service("s5", CASA_ID, "Uñas esculpidas", ServiceCategory.UNAS, ServiceModality.AMBOS)

    val services = listOf(corteYBarba, masaje, corteDeUnas, limpieza, heladeras, unasEsculpidas)

    val entries: List<CatalogEntry> = services.map { service ->
        CatalogEntry(service, if (service.providerId == STUDIO_ID) studio else casa)
    }

    /** A completed, rated Booking of [providerId]; [completedAt] orders the reviews. */
    fun ratedBooking(id: String, providerId: String, stars: Int, comment: String?, completedAt: String) = Booking(
        id = id,
        providerId = providerId,
        serviceId = "s1",
        customerId = "c1",
        scheduledAt = LocalDateTime.parse("2026-09-01T10:00:00"),
        durationMinutesSnapshot = 60,
        priceCentsSnapshot = 4000,
        requestedAt = Instant.parse("2026-08-30T10:00:00Z"),
        status = BookingStatus.COMPLETED,
        rating = Rating(stars, comment),
        completedAt = Instant.parse(completedAt),
    )
}
