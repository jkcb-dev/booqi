package corp.khin.solutions.booqi.domain.usecase

import corp.khin.solutions.booqi.core.common.DomainError

/**
 * Input rules shared by the Identity use cases. Each returns the [DomainError.InvalidInput] to
 * hand back, or `null` when the value is fine — the use case runs these *before* any repository
 * call (docs/DEVELOPMENT.md § Validation failures).
 */
internal const val MIN_PASSWORD_LENGTH = 8

internal fun validateDisplayName(name: String): DomainError.InvalidInput? =
    if (name.isBlank()) DomainError.InvalidInput("El nombre es obligatorio") else null

/** Deliberately loose (`a@b.c`): the real check is the verification email, not a regex. */
internal fun validateEmail(email: String): DomainError.InvalidInput? {
    val local = email.substringBefore('@', missingDelimiterValue = "")
    val domain = email.substringAfter('@', missingDelimiterValue = "")
    return when {
        email.isBlank() -> DomainError.InvalidInput("El correo es obligatorio")
        local.isEmpty() || !domain.contains('.') || domain.startsWith('.') || domain.endsWith('.') ||
            email.any { it.isWhitespace() } || email.count { it == '@' } != 1 ->
            DomainError.InvalidInput("El correo no es válido")
        else -> null
    }
}

internal fun validateNewPassword(password: String): DomainError.InvalidInput? =
    if (password.length < MIN_PASSWORD_LENGTH) {
        DomainError.InvalidInput("La contraseña debe tener al menos $MIN_PASSWORD_LENGTH caracteres")
    } else {
        null
    }

/** Emails are compared case-insensitively; the stored form is trimmed and lowercase. */
internal fun String.normalizedEmail(): String = trim().lowercase()
