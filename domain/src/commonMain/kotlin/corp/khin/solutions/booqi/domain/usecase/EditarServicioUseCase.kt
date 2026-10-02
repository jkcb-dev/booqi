package corp.khin.solutions.booqi.domain.usecase

import corp.khin.solutions.booqi.core.common.DomainError
import corp.khin.solutions.booqi.core.common.DomainResult
import corp.khin.solutions.booqi.core.common.asFailure
import corp.khin.solutions.booqi.domain.model.Service
import corp.khin.solutions.booqi.domain.model.ServiceDetails
import corp.khin.solutions.booqi.domain.repository.ServiceRepository

/**
 * Escenario: "El Proveedor edita un Servicio existente" (docs/domain/provider-flow.md § Grupo 2).
 *
 * Scope decision: the Gherkin calls out "modifica su precio o duración" as the example, but a
 * real edit screen lets a Provider correct any of the fields they set when adding the Service
 * (title, photo, description, price, duration, modality) — restricting this use case to only
 * price/duration would be an arbitrary API restriction the scenario doesn't actually ask for, and
 * would force a second "EditarDetallesServicio"-style use case later for no domain reason. What
 * this use case deliberately does *not* let a caller change: [Service.id] (identity),
 * [Service.providerId] (ownership — reassigning a Service to another Provider isn't an "edit"),
 * and [Service.isActive] (that's [DeshabilitarServicioUseCase]'s single responsibility, not an
 * edit-form field).
 *
 * Re-validates `photoUrl.isBlank()` the same way [AgregarServicioUseCase] does — "la foto es
 * obligatoria" is an invariant of a valid [Service], not just a create-time check; without
 * re-validating here, an edit could blank out the photo of an already-published Service.
 *
 * **Snapshots, not live references**: the scenario's "las citas ya reservadas conservan el
 * precio/duración original acordado" is about `Booking` (which doesn't exist as an entity yet —
 * separate ticket #18/#25/#26) snapshotting the price/duration at the time of booking, not about
 * `Service` itself. This use case only updates the current `Service` row going forward — it has
 * no dependency on `Booking` and must not gain one speculatively here.
 */
class EditarServicioUseCase(
    private val repository: ServiceRepository,
) {
    suspend operator fun invoke(serviceId: String, details: ServiceDetails): DomainResult<Service> {
        if (details.photoUrl.isBlank()) {
            return DomainError.InvalidInput("La foto es obligatoria").asFailure()
        }
        return repository.updateService(serviceId, details)
    }
}
