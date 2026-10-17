package corp.khin.solutions.booqi.feature.account

/**
 * Immutable state of the sign-in form. Validation failures from the use case land on the field
 * they are about ([emailError], [passwordError]); wrong credentials and every other failure are
 * [formError] — a message on the form, never a crash or a generic error event.
 */
data class SignInUiState(
    val email: String = "",
    val password: String = "",
    val emailError: String? = null,
    val passwordError: String? = null,
    val formError: String? = null,
    val isSubmitting: Boolean = false,
)
