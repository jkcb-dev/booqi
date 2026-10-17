package corp.khin.solutions.booqi.domain.model

/**
 * Why a Booking was rejected or cancelled: a predefined [code] plus an optional free-text [note]
 * (the "Otro" option's text, but any code may carry one). Value object; the status of the owning
 * [Booking] tells whether it is a rejection or a cancellation.
 *
 * The *list* of codes depends on who acts, so [ReasonCode] is a sealed interface with one enum per
 * actor: [ProviderReasonCode] (Grupo 4, docs/domain/provider-flow.md) and [CustomerReasonCode]
 * (Grupo 3, docs/domain/customer-flow.md, #25). Notifying the other party of the reason is
 * deferred — no notification system exists yet.
 */
data class Reason(
    val code: ReasonCode,
    val note: String? = null,
) {
    companion object {
        /** Builds a [Reason] normalizing a blank [note] to `null` (the text is optional). */
        fun of(code: ReasonCode, note: String?): Reason =
            Reason(code, note?.trim()?.takeIf { it.isNotEmpty() })
    }
}

/**
 * A predefined reason option. Sealed so the set of actors is closed; [Booking]'s transitions take
 * the acting side's own enum (e.g. [ProviderReasonCode]) so the wrong list can't be passed. [storageCode] is the
 * stable value stored in `bookings.reason_code` (docs/DATABASE.md); it is unique across all
 * implementations so a stored code identifies its enum on its own.
 */
sealed interface ReasonCode {
    val storageCode: String
}

/**
 * The Provider's predefined reasons, shared by rejecting a request and cancelling an accepted
 * appointment (docs/domain/provider-flow.md § Grupo 4; the cancellation reuse is an assumption
 * recorded in docs/DOMAIN.md). Wording shown to users lives in the UI layer.
 */
enum class ProviderReasonCode(override val storageCode: String) : ReasonCode {
    /** "No disponible en este horario". */
    NOT_AVAILABLE_AT_THIS_TIME("provider_not_available_at_this_time"),

    /** "Fuera de mi zona de servicio". */
    OUTSIDE_SERVICE_AREA("provider_outside_service_area"),

    /** "Servicio no disponible temporalmente". */
    SERVICE_TEMPORARILY_UNAVAILABLE("provider_service_temporarily_unavailable"),

    /** "Otro" — the free-text [Reason.note] is optional. */
    OTHER("provider_other"),
}

/**
 * The Customer's predefined reasons for cancelling a confirmed appointment (docs/domain/customer-flow.md
 * § Grupo 3) — deliberately a different list from [ProviderReasonCode]. Wording shown to users
 * lives in the UI layer.
 */
enum class CustomerReasonCode(override val storageCode: String) : ReasonCode {
    /** "Cambio de planes". */
    CHANGE_OF_PLANS("customer_change_of_plans"),

    /** "Encontré otro Proveedor". */
    FOUND_ANOTHER_PROVIDER("customer_found_another_provider"),

    /** "Ya no necesito el servicio". */
    NO_LONGER_NEEDED("customer_no_longer_needed"),

    /** "Otro" — the free-text [Reason.note] is optional. */
    OTHER("customer_other"),
}
