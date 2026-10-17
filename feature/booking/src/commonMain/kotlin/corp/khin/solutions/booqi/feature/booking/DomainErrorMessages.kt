package corp.khin.solutions.booqi.feature.booking

import corp.khin.solutions.booqi.core.common.DomainError

/** User-facing Spanish text for a [DomainError]; [notFound] is what `NotFound` means in context. */
internal fun DomainError.describe(notFound: String): String = when (this) {
    is DomainError.InvalidInput -> message
    is DomainError.Unknown -> message ?: "Ocurrió un error inesperado"
    DomainError.NoConnection -> "Sin conexión"
    DomainError.Timeout -> "La operación tardó demasiado"
    DomainError.NotFound -> notFound
    DomainError.Unauthorized -> "No autorizado"
}
