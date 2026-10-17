package corp.khin.solutions.booqi.feature.account

/** The inputs of the sign-in / sign-up forms that a validation message can belong to. */
internal enum class AuthFormField { NAME, EMAIL, PASSWORD }

/**
 * Which field an `InvalidInput` message from the Identity use cases is about, or `null` for a
 * message about the form as a whole. `DomainError.InvalidInput` carries only text, no field, so
 * this reads the subject the use cases put first in every message ("El nombre es obligatorio",
 * "El correo no es válido", "La contraseña debe tener...", "Ya existe una cuenta con ese correo").
 * An unrecognized message falls back to the form level, which still shows it — nothing is lost.
 */
internal fun fieldOf(message: String): AuthFormField? = when {
    message.startsWith("El nombre") -> AuthFormField.NAME
    message.startsWith("La contraseña") -> AuthFormField.PASSWORD
    message.contains("correo") -> AuthFormField.EMAIL
    else -> null
}
