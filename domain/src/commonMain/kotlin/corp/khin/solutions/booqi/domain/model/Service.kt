package corp.khin.solutions.booqi.domain.model

/**
 * The *what* — a specific offering a Provider provides (docs/DOMAIN.md — Provider Management
 * bounded context). Plain data class, no serialization annotations — those belong on the DTO in
 * `data`, never here.
 *
 * References its owning `ProviderProfile` by [providerId] only — it never embeds a
 * `ProviderProfile` object. `Service` and `ProviderProfile` are two separate aggregates with two
 * separate lifecycles (docs/DOMAIN.md § Aggregate boundaries); the same rule applies to `Booking`
 * referencing this entity by `serviceId` (`Booking`, #18), never by embedding.
 *
 * Lifecycle (docs/domain/provider-flow.md § Grupo 2):
 * - Created by [corp.khin.solutions.booqi.domain.usecase.AgregarServicioUseCase], which requires
 *   [photoUrl] to be non-blank.
 * - Updated going forward by [corp.khin.solutions.booqi.domain.usecase.EditarServicioUseCase] —
 *   this only changes the current row; it does not and must not retroactively change what an
 *   already-booked `Booking` snapshot recorded (that snapshotting is `Booking`'s concern, not
 *   `Service`'s — see [EditarServicioUseCase] KDoc).
 * - Soft-deleted (never hard-deleted) by
 *   [corp.khin.solutions.booqi.domain.usecase.DeshabilitarServicioUseCase], which flips
 *   [isActive] to `false` so historical `Booking.serviceId` references never dangle.
 * - Re-enabled by [corp.khin.solutions.booqi.domain.usecase.HabilitarServicioUseCase], the inverse
 *   flip back to `true` (still soft — nothing else changes).
 * - Read by [corp.khin.solutions.booqi.domain.usecase.ObtenerServiciosDelProveedorUseCase] (the
 *   Provider's own list, disabled included) and
 *   [corp.khin.solutions.booqi.domain.usecase.ObtenerServicioUseCase] (one by id).
 */
data class Service(
    val id: String,
    val providerId: String,
    val title: String,
    val photoUrl: String,
    val description: String,
    val priceCents: Int,
    val durationMinutes: Int,
    val modality: ServiceModality,
    val isActive: Boolean = true,
)

/**
 * The Provider-editable fields of a [Service] — what the add/edit form submits. Groups them into
 * one value so add/edit signatures stay small (detekt `LongParameterList`) and so "what a Provider
 * can set" is defined in one place. Excludes identity ([Service.id]), ownership
 * ([Service.providerId]) and lifecycle ([Service.isActive]), which are never form fields.
 */
data class ServiceDetails(
    val title: String,
    val photoUrl: String,
    val description: String,
    val priceCents: Int,
    val durationMinutes: Int,
    val modality: ServiceModality,
)

/** Where a [Service] is delivered: at the Provider's location, the Customer's, or both. */
enum class ServiceModality {
    LOCAL,
    DOMICILIO,
    AMBOS,
}
