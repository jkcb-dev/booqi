package corp.khin.solutions.booqi.feature.provider

import corp.khin.solutions.booqi.core.common.DomainError

/** User-facing Spanish text for a [DomainError], shared by the service ViewModels and screens.
 * [notFound] is what `NotFound` means in the calling context ("No se encontró el servicio"). */
internal fun DomainError.describe(notFound: String): String = when (this) {
    is DomainError.InvalidInput -> message
    is DomainError.Unknown -> message ?: "Ocurrió un error inesperado"
    DomainError.NoConnection -> "Sin conexión"
    DomainError.Timeout -> "La operación tardó demasiado"
    DomainError.NotFound -> notFound
    DomainError.Unauthorized -> "No autorizado"
}
