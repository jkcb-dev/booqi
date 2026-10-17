package corp.khin.solutions.booqi.feature.booking

import corp.khin.solutions.booqi.core.designsystem.component.ReasonOption
import corp.khin.solutions.booqi.domain.model.CustomerReasonCode
import corp.khin.solutions.booqi.domain.model.ProviderReasonCode
import corp.khin.solutions.booqi.domain.model.Reason

/** The wording of each predefined Customer cancellation reason (the domain keeps only the codes). */
internal fun CustomerReasonCode.label(): String = when (this) {
    CustomerReasonCode.CHANGE_OF_PLANS -> "Cambio de planes"
    CustomerReasonCode.FOUND_ANOTHER_PROVIDER -> "Encontré otro proveedor"
    CustomerReasonCode.NO_LONGER_NEEDED -> "Ya no necesito el servicio"
    CustomerReasonCode.OTHER -> "Otro"
}

/** The wording of each predefined Provider reason, as a Customer reads it on a rejection. */
internal fun ProviderReasonCode.label(): String = when (this) {
    ProviderReasonCode.NOT_AVAILABLE_AT_THIS_TIME -> "No disponible en este horario"
    ProviderReasonCode.OUTSIDE_SERVICE_AREA -> "Fuera de la zona de servicio"
    ProviderReasonCode.SERVICE_TEMPORARILY_UNAVAILABLE -> "Servicio no disponible temporalmente"
    ProviderReasonCode.OTHER -> "Otro"
}

/** The options `ReasonPicker` lists for the Customer's cancellation; "Otro" alone reveals the note. */
internal val customerReasonOptions: List<ReasonOption> = CustomerReasonCode.entries.map {
    ReasonOption(id = it.name, label = it.label(), allowsNote = it == CustomerReasonCode.OTHER)
}

/** Maps a picker option id back to its code. */
internal fun customerReasonFor(id: String): CustomerReasonCode? =
    CustomerReasonCode.entries.firstOrNull { it.name == id }

/** The reason's label for either side's code. */
internal fun Reason.label(): String = when (val c = code) {
    is ProviderReasonCode -> c.label()
    is CustomerReasonCode -> c.label()
}
