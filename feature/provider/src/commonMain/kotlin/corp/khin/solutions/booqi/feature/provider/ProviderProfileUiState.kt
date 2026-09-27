package corp.khin.solutions.booqi.feature.provider

import corp.khin.solutions.booqi.core.common.DomainError
import corp.khin.solutions.booqi.domain.model.ProviderProfile
import kotlinx.datetime.LocalDate

/**
 * Immutable, single source of truth for the Provider Profile screen. Survives recomposition
 * as-is. Covers all three steps of the flow (docs/design/SCREENS.md P1/P2/P3) as one continuous
 * state, driven by [profile]:
 * - `profile == null` -> P1 "activar modo" CTA.
 * - `profile != null && !profile.isComplete` -> P2 "completar perfil" form.
 * - `profile != null && profile.isComplete` -> profile summary, with the P3 "pausar perfil"
 *   action available from there (see [isPauseSheetVisible]).
 */
data class ProviderProfileUiState(
    val isLoading: Boolean = false,
    val profile: ProviderProfile? = null,

    // P2 — "completar perfil" form fields, editable independently of the persisted [profile].
    val nameInput: String = "",
    val photoUrlInput: String = "",
    val descriptionInput: String = "",
    val locationInput: String = "",
    val isSaving: Boolean = false,
    /** Surfaced under the ubicación field — "El Proveedor intenta completar el perfil sin
     * ubicación" must render as a form error, not a crash. */
    val locationError: String? = null,

    // P3 — "pausar perfil" date-range action, reached in-screen once the profile is complete.
    val isPauseSheetVisible: Boolean = false,
    val pauseFrom: LocalDate? = null,
    val pauseUntil: LocalDate? = null,
    val pauseRangeError: String? = null,
    val isPausing: Boolean = false,

    val error: DomainError? = null,
)
