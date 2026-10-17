package corp.khin.solutions.booqi.feature.account

import corp.khin.solutions.booqi.core.common.DomainError
import corp.khin.solutions.booqi.domain.model.User

/**
 * Immutable state of the user profile screen. [user] is `null` for a visitor browsing without an
 * account (a normal state, not an error): the screen then offers to sign in or sign up.
 *
 * [hasProviderProfile] only changes the wording of the Provider entry ("Convertirme en Proveedor"
 * vs "Ir a mi panel de Proveedor"). The name/photo form is open while [isEditing]; [nameError] is
 * the use case's "El nombre es obligatorio". [error] is a failed load; [actionError] is why the
 * last save / sign-out / Provider-entry attempt failed; [notice] is a positive message.
 */
data class ProfileUiState(
    val isLoading: Boolean = true,
    val user: User? = null,
    val hasProviderProfile: Boolean = false,
    val isEditing: Boolean = false,
    val nameInput: String = "",
    val photoUrlInput: String = "",
    val nameError: String? = null,
    val isSaving: Boolean = false,
    val isSigningOut: Boolean = false,
    val notice: String? = null,
    val actionError: String? = null,
    val error: DomainError? = null,
)
