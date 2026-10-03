package corp.khin.solutions.booqi.core.navigation

/**
 * Every screen in the app, in one closed set owned by this module — not by any `feature:*`
 * module. Feature modules depend on [Navigator] (and reference destinations *other* features own
 * only through this sealed type), so `feature:browse` never has a compile-time dependency on
 * `feature:booking`'s screen classes.
 *
 * Grouped below by which `feature:*` module owns rendering it, per the module policy in
 * `docs/ARCHITECTURE.md` (`feature:browse` exists today; `feature:booking` and `feature:provider`
 * are planned homes — this sealed type is expanded ahead of those modules existing, per
 * `Navigator`'s own doc comment: "wiring a new one is additive here, not a rewrite").
 */
sealed interface Destination {

    // --- feature:browse — Catalog/discovery (Customer) ---------------------------------------

    data object Browse : Destination

    /** Detalle de un Servicio (C3, `VerDetalleServicio`, customer-flow.md Grupo 1). Renamed from
     * `ProviderDetail` once the Provider/Service split landed (#20/#21) — it always rendered a
     * Service. [ProviderProfileView] below is the Provider's public profile (C4). */
    data class ServiceDetail(val serviceId: String) : Destination

    /** Perfil completo de Proveedor (`VerPerfilProveedor`, customer-flow.md Grupo 1) — every
     * Service the Provider offers plus their aggregate rating, distinct from a single Service's
     * detail ([ServiceDetail]). Reached by tapping the Provider's name/photo from there. */
    data class ProviderProfileView(val providerId: String) : Destination

    // --- feature:booking — Scheduling + address (Customer) ------------------------------------

    data class Booking(val providerId: String) : Destination
    data object BookingConfirmation : Destination

    /** Historial de reservas (`VerHistorialReservas`, customer-flow.md Grupo 3) — the Customer's
     * Bookings across all `BookingStatus` values (pending, confirmed, completed, cancelled).
     * Named `MyBookings` from an earlier ticket; kept as-is rather than duplicated per this
     * issue's "check whether it already exists" rule. */
    data object MyBookings : Destination

    /** Selección/edición de dirección (`AgregarDireccion` / `ActualizarDireccion`,
     * customer-flow.md Grupo 2). One screen for both add and edit — the Customer has at most one
     * saved address (see `docs/DOMAIN.md`'s Dirección entry), so "select" and "edit" are the same
     * map/search UI. Reached either proactively from the Customer's profile or reactively
     * mid-booking; the reached-from context is feature-internal state, not part of this ID-free
     * destination. */
    data object AddressSelection : Destination

    // --- feature:provider — Provider Management + booking-request inbox (Provider) ------------

    /** Activar/completar perfil de Proveedor (`ActivarModoProveedor` +
     * `CompletarPerfilDeProveedor`, provider-flow.md Grupo 1). One screen covers both — activating
     * creates an empty `ProviderProfile` and immediately continues into completing it. Scoped to
     * the current signed-in User, so no ID parameter. */
    data object ProviderProfileSetup : Destination

    /** Gestión de Servicios — list (provider-flow.md Grupo 2). Entry point for a Provider's own
     * Services; navigates to [ServiceEditor] to add or edit one. */
    data object ServiceList : Destination

    /** Gestión de Servicios — add/edit (`AgregarServicio` / `EditarServicio`, provider-flow.md
     * Grupo 2). `serviceId == null` means create-new; a non-null id means editing that existing
     * Service. Disabling a Service (`DeshabilitarServicio`) is an action within this screen, not a
     * separate destination — see `docs/DOMAIN.md`'s soft-delete note. */
    data class ServiceEditor(val serviceId: String? = null) : Destination

    /** Gestión de Horario (`DefinirHorarioSemanal` / `ModificarHorarioSemanal` /
     * `BloquearFechaHora`, provider-flow.md Grupo 3). Weekly availability and specific
     * date/time blocks are edited on this one screen. Scoped to the current Provider, no ID
     * parameter. */
    data object ScheduleManagement : Destination

    /** Bandeja de solicitudes de reserva pendientes, lado Proveedor (`SolicitarReserva` inbound +
     * `AceptarReserva` / `RechazarReserva`, provider-flow.md Grupo 4). Lists a Provider's
     * `Requested` Bookings; accept/reject-with-reason are actions within this screen. Scoped to
     * the current Provider, no ID parameter. */
    data object BookingRequestInbox : Destination
}
