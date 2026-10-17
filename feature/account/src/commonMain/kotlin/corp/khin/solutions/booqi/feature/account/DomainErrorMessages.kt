package corp.khin.solutions.booqi.feature.account

import corp.khin.solutions.booqi.core.common.DomainError

/**
 * User-facing Spanish text for a [DomainError]. [unauthorized] is what `Unauthorized` means in the
 * calling context: wrong credentials on the sign-in form, "your session ended" everywhere else.
 */
internal fun DomainError.describe(unauthorized: String = SESSION_ENDED_MESSAGE): String = when (this) {
    is DomainError.InvalidInput -> message
    is DomainError.Unknown -> message ?: "Ocurrió un error inesperado"
    DomainError.NoConnection -> "Sin conexión"
    DomainError.Timeout -> "La operación tardó demasiado"
    DomainError.NotFound -> "No encontramos la cuenta"
    DomainError.Unauthorized -> unauthorized
}

internal const val SESSION_ENDED_MESSAGE = "Tu sesión terminó. Iniciá sesión de nuevo"
