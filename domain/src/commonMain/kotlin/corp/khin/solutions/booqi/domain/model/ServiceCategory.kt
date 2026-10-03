package corp.khin.solutions.booqi.domain.model

/**
 * What kind of offering a [Service] is — the C1 category chips (docs/design/SCREENS.md:
 * Todos/Barber/Uñas/Limpieza/Masajes/Técnico). "Todos" is not a value: it is the absence of a
 * category filter ([ServiceSearchCriteria.category] `null`).
 *
 * [OTRO] is the default for a Service whose Provider hasn't picked one (the Servicios editor has no
 * category picker yet, see the follow-up on #20) and for unknown stored values. It still shows up
 * under "Todos" but has no chip of its own — see [filterable].
 */
enum class ServiceCategory {
    BARBERIA,
    UNAS,
    LIMPIEZA,
    MASAJES,
    TECNICO,
    OTRO,
    ;

    companion object {
        /** The categories that get a chip on C1, in chip order (everything except [OTRO]). */
        val filterable: List<ServiceCategory> = listOf(BARBERIA, UNAS, LIMPIEZA, MASAJES, TECNICO)
    }
}
