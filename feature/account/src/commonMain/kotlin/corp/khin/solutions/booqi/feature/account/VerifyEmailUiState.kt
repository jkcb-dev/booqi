package corp.khin.solutions.booqi.feature.account

import corp.khin.solutions.booqi.core.common.DomainError

/**
 * Immutable state of the "verificá tu correo" notice. [email] is where the verification message
 * went (`null` until loaded). [notice] is a positive message ("Te enviamos el correo otra vez");
 * [error] is why the last action failed. They are never both set.
 */
data class VerifyEmailUiState(
    val isLoading: Boolean = true,
    val email: String? = null,
    val isChecking: Boolean = false,
    val isResending: Boolean = false,
    val notice: String? = null,
    val error: DomainError? = null,
)
