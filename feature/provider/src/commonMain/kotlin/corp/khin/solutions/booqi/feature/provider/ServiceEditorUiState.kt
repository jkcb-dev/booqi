package corp.khin.solutions.booqi.feature.provider

import corp.khin.solutions.booqi.core.common.DomainError
import corp.khin.solutions.booqi.domain.model.ServiceModality

/**
 * Immutable state of the add/edit service form (docs/design/SCREENS.md P5). [serviceId] `null`
 * means "add"; non-null means "edit", in which case the inputs are preloaded from the stored
 * service once [isLoading] turns false.
 *
 * Price and duration are kept as the raw text the Provider typed ([priceInput]/[durationInput])
 * and only converted to `priceCents`/`durationMinutes` on save, so a half-typed value is never
 * lost or reformatted under the cursor.
 */
data class ServiceEditorUiState(
    val serviceId: String? = null,
    /** Loading the existing service to edit. Always false in "add" mode. */
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,

    val photoUrlInput: String = "",
    val titleInput: String = "",
    val descriptionInput: String = "",
    val priceInput: String = "",
    val durationInput: String = "",
    val modality: ServiceModality = ServiceModality.LOCAL,

    /** "La foto es obligatoria" — comes back from the use case as `DomainError.InvalidInput`. */
    val photoError: String? = null,
    val priceError: String? = null,
    val durationError: String? = null,

    /** Non-form failure (e.g. `NotFound` for an unknown service id, no connection). */
    val error: DomainError? = null,
) {
    val isEditing: Boolean get() = serviceId != null
}
