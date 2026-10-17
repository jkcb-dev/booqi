package corp.khin.solutions.booqi.feature.account

/**
 * Immutable state of the sign-up form. A rejected field ("El nombre es obligatorio", an email that
 * is already taken, a short password) is shown on that field; anything else is [formError].
 */
data class SignUpUiState(
    val name: String = "",
    val email: String = "",
    val password: String = "",
    val nameError: String? = null,
    val emailError: String? = null,
    val passwordError: String? = null,
    val formError: String? = null,
    val isSubmitting: Boolean = false,
)
