# Booqi Domain Model (DDD)

**Corrected 2026-08-11** — the original version of this document conflated Provider and Service
into one entity. A product discovery session (see `docs/domain/provider-flow.md`) clarified that
they're separate concepts with separate lifecycles. This version replaces that one; nothing below
should be assumed to match the initial scaffold's `ServiceProvider` type, which was wrong. The
Catalog runs on the split model (#20) and `feature:browse` is built on it (#21); the old
`ServiceProvider` cluster has been deleted.

Scope note, unchanged from before: this is **tactical DDD** (ubiquitous language, aggregates,
value objects, invariants), not full strategic DDD. Bounded contexts stay as packages inside the
existing modules, not separate Gradle modules, unless a context's pace of change actually diverges
enough to justify the ceremony.

## Ubiquitous language

| Term | Meaning |
|---|---|
| **User** | An account (`User`, Identity context). Authenticates via Google, Facebook, Apple ID, or email/password; email/password accounts must verify their email before booking or activating Provider mode. Any User can book services (customer capability is implicit); a User optionally also has a **ProviderProfile** if they choose to offer services. Being a customer and a provider is not an exclusive choice — one account can be both. |
| **ProviderProfile** | The identity of *who* offers services: display name, photo, description, location (a text line plus optional `coordinates`), aggregate rating, weekly availability. One optional ProviderProfile per User. |
| **Service** | The *what* — a specific offering a Provider provides, with its own title, photo, description, price, duration, modality (Local / Domicilio / both) and **category**. A ProviderProfile owns one or more Services. **A Provider is not a Service — this was the original modeling mistake.** |
| **ServiceCategory** | What kind of Service it is — the C1 chips: `BARBERIA`, `UNAS`, `LIMPIEZA`, `MASAJES`, `TECNICO`, plus `OTRO` (default when the Provider hasn't chosen one; no chip, only found under "Todos"). Lives on the `Service`, not the Provider. |
| **GeoPoint** | A latitude/longitude pair (value object) — the lat/lng half of `Address`. `ProviderProfile.coordinates` (optional, `provider_profiles.location_lat/lng`) and the Customer's GPS fix are compared with a simple great-circle distance. |
| **Modality** | Whether a Service is delivered at the Provider's location (**Local**) or the Customer's (**Domicilio**), or both. |
| **TimeSlot** | A specific bookable unit of time for a Provider. Value object — equality by value (`providerId` + date + start time), no identity of its own. |
| **Availability** | A Provider's recurring weekly schedule (one range per day of the week) plus specific blocked dates/times. Value object, references its Provider by `providerId`. The optional "paused" date range (vacation mode) lives on `ProviderProfile.pausedRange` and is passed in alongside when TimeSlots are generated from this. |
| **Booking** | A Customer's request to reserve a Provider's TimeSlot for a specific Service. Aggregate root for the Scheduling context. Goes through a request→accept/reject lifecycle — see `docs/domain/provider-flow.md` and `docs/domain/customer-flow.md` for the full state machine. |
| **BookingStatus** | `Requested → Confirmed → Completed`, or `Requested → Rejected` / `Requested → Expired` (24h no response), or `Confirmed → CancelledByProvider` / `Confirmed → CancelledByCustomer` (up to 3h before the appointment). No other transitions are valid. In code (`BookingStatus`: `REQUESTED`, `CONFIRMED`, …) the transition table lives in `BookingStatus` and every change goes through `Booking`'s transition functions, which reject invalid ones with `InvalidInput`. |
| **Dirección (Address)** | A single saved address on `User`, used for `Domicilio`-modality bookings. Added via Google Maps search or map-pin selection — proactively from the profile, or reactively the first time it's needed at booking time. Only one is kept (not a list of saved places). In code it is the small `Address` value object (display line + latitude/longitude), introduced by `Booking` (#18) for the delivery-address *snapshot*; the Customer's saved address (#22) reuses it. |
| **Reason** | Why a Booking was rejected or cancelled: a predefined code + optional free text. The list of codes depends on who acts — `ProviderReasonCode` today, a Customer list in #25, both under the sealed `ReasonCode`. |
| **Rating** | A Customer's 1–5 stars + optional comment on a completed Booking. Embedded in `Booking`, not an aggregate. |

## Bounded contexts

**Identity** — the `User` account itself: authentication, whether a ProviderProfile exists for
this user. The product rules (browse without an account, sign-in only to book or become a Provider,
email verification, account deletion with anonymized history) are specified in
`docs/domain/identity-flow.md` (issue #50). **Partially built — domain & data (#56):** `User` (+
`SocialProvider`, `PublicUser`), `RegistrarUsuario` (email+password or Google/Facebook/Apple),
`IniciarSesion`, `CerrarSesion`, `ObtenerUsuarioActual` (`null` while browsing), `VerificarCorreo` +
`ReenviarCorreoDeVerificacion`, `ActualizarPerfilDeUsuario`, `EliminarCuenta`, `ObtenerUsuarioPublico`
and the access guard `RequerirCuentaVerificada` (booking and Provider mode need a session **and** a
verified email: no session -> `Unauthorized`, unverified -> `InvalidInput`). Persistence is a
TEMPORARY in-memory fake auth (`FakeAuthRemoteDataSource`) until Supabase Auth (#27) — real
authentication is Supabase's job. **Not built:** the screens, applying the guard in front of
`SolicitarReserva`/`ActivarModoProveedor`, and aligning the Provider screens' ids (the UI
sub-tickets of #50). **Account deletion** never removes the `User`: it is kept as an anonymized
tombstone (`User.anonymized()` — name, photo and email erased, `isDeleted = true`), and
`User.publicName` is the one place that renders it as "Usuario eliminado". Completed Bookings and
their ratings stay (so a Provider's rating aggregate never changes), the personal data on the
Customer's Bookings (delivery-address snapshot, free-text note) is erased, and a ProviderProfile is
anonymized and set `isComplete = false` — the existing gate that removes it and its Services from
Catalog search and public pages. It is refused while a Booking is `REQUESTED`/`CONFIRMED` as Customer
(`customerId == user.id`) or as Provider (`providerId ==` the user's ProviderProfile id).

**Provider Management** — a Provider's own "back office": profile, Services (create/edit/disable),
Availability (define/modify schedule, block dates, pause profile). Fully specified in
`docs/domain/provider-flow.md`. **Partially built:** Grupo 1 (perfil: activar/completar/pausar —
domain, data and `feature:provider` UI) and Grupo 2's domain/data (`Service`, add/edit/disable use
cases; its UI is #15) and Grupo 3's domain/data (`Availability`, define/modify weekly hours,
block/unblock, TimeSlot generation; its UI is #17). The Provider side of Bookings (Grupo 4) lives in
the Scheduling context below. All datasources are still in-memory fakes until #27.

**Catalog** — browsing/discovery, read-heavy. Searches across Services (not Providers directly),
filterable by category, free text and distance (GPS-based, simple radius — no polygon zones).
**Domain & data built (#20):** `BuscarServiciosUseCase` (text/category/distance; a Service is a
candidate only if it is active, its profile complete and not paused *today*; ordered by distance,
then title, then id), `VerDetalleServicioUseCase` (C3), `VerPerfilProveedorUseCase` (C4: profile +
active Services + rating aggregate + reviews, reusing `ObtenerCalificacionesDelProveedor`), and the
chip list (`ServiceCategory.filterable`). They return read models (`ServiceSearchResult`,
`ServiceDetail`, `ProviderPublicProfile`, ...) and read through `CatalogRepository`, which joins
`Service.providerId == ProviderProfile.id` — see "providerId contract" below. The Customer's
search/detail/profile UI is #21 (`feature:browse`; it searches from a TEMPORARY fixed location until
platform location exists, #24). **Not built:** a category picker in the Servicios editor and a way for a Provider to set `coordinates` (follow-ups
of #20 — until then every Service is `OTRO` and every profile has no coordinates, so category and
distance filters only find sample data).

**providerId contract (#50, settled in #56).** `Service`/`Availability`/`TimeSlot`/`Booking`.`providerId`
**is** a `ProviderProfile.id`, never a `User.id`; `Booking.customerId` and `ProviderProfile.userId`
are `User.id`s. The user -> profile path is `ProviderProfileRepository.findByUserId` /
`ObtenerMiPerfilDeProveedorUseCase` (`null` when the user never activated Provider mode), and
`ActivarModoProveedor` returns the profile whose `id` the Provider screens must use. The Catalog
follows the contract strictly and has no fallback by `userId`. **Until the Provider screens are
changed** (UI sub-ticket of #50), they and `SampleData`'s provider-side rows still pass a user
placeholder, so those Services don't join (and don't appear in search).

**Scheduling** — the Booking lifecycle: request, accept/reject/expire, complete, cancel, rate.
Fully specified in `docs/domain/provider-flow.md`'s "Gestión de Reservas y Calificaciones" group.
**Domain & data built (#18):** the `Booking` aggregate and its state machine, `SolicitarReserva`,
accept/reject/complete/provider-cancel, the 24h expiry sweep (`ExpirarSolicitudesVencidas` — the
scheduling *trigger* is deliberately not built, see `docs/ARCHITECTURE.md` § Booking expiry
trigger), `CalificarCita` with the Provider's rating recomputation, the available-TimeSlots query
and the Provider's queries (inbox, detail, agenda, reviews). **Provider UI built (#19):** the booking
inbox/agenda with accept, reject, complete and cancel, and the received ratings on the profile.
**Not built:** the Customer's `CancelarReservaCliente`/`VerHistorialReservas` (#25), any
notification, and the Customer's UI.

## Aggregate boundaries — the rule that matters

Unchanged from the original version of this doc: **`Booking` references `Service` and
`ProviderProfile` by ID, never by embedding them.** Different aggregates, different lifecycles — a
Service's price changing shouldn't retroactively change what a past Booking says the customer
agreed to pay.

```
Booking (aggregate root)
├─ id: String
├─ providerId: String      ← reference (= ProviderProfile.id)
├─ serviceId: String       ← reference
├─ customerId: String      ← reference
├─ scheduledAt: LocalDateTime  ← provider-local date + start time (= timeSlot), no timezone yet
├─ durationMinutesSnapshot / priceCentsSnapshot: Int  ← copied from Service at request time
├─ deliveryAddress: Address?  ← snapshot copied at request time, only if Service modality = Domicilio; never a live reference to User.direccion
├─ customerNote: String?
├─ status: BookingStatus
├─ reason: Reason?          ← rejection or cancellation (the status tells which): predefined code + optional free text
├─ rating: Rating?          ← stars + optional comment, set only once status = Completed, only once
└─ requestedAt / respondedAt / completedAt: Instant
```

`Rating` is modeled as an optional field on `Booking` itself, not a separate aggregate — a rating
is 1:1 with a completed Booking and has no independent lifecycle of its own. A Provider's overall
rating shown on their profile is a *computed aggregate* (average across all their Bookings'
ratings), not a stored field that gets manually updated. `ProviderProfile.ratingAverage`/`ratingCount` are
a persisted *copy* of that computation, recomputed from **all** the Provider's rated Bookings every
time a rating is left (never incremented), so they cannot drift.

## Deliberate scope decisions for V1 (recorded so they don't get silently re-litigated)

- **Rating and Availability live on `ProviderProfile`, not per-`Service`.** Splitting them per
  service would fragment review/rating signal too thin early on, and most providers are one person
  with one schedule regardless of which service is being performed. Revisit only if
  multi-staff-per-provider becomes a real need.
- **Replying to reviews is deferred (V2)** — not essential to the core loop (search → book →
  complete → rate), and depends on Provider Management UI that doesn't exist yet.
- **Suggesting a Provider update their schedule after a rejection is deferred (V2)** — a nice
  nudge, not core functionality.
- **Service deletion is soft (disable), not hard delete** — a hard delete would orphan any
  `Booking.serviceId` referencing it, including completed bookings that are part of a Customer's
  and Provider's history. Disabling just hides it from Catalog search going forward.
- **Changing a Provider's schedule or disabling a Service does not retroactively cancel already-
  accepted Bookings** — a customer's confirmed appointment stays honored even if the Provider's
  general availability changes afterward.
- **Payments are out of scope for V1** — Bookings reach `Confirmed` without any payment step.
  Revisit when payments are designed; it will likely insert a state between `Requested` and
  `Confirmed`, or attach to `Confirmed`, not replace the flow above.
- **Cancellation reason options are assumed to reuse the same predefined list as rejection**
  ("No disponible en este horario" / "Fuera de mi zona de servicio" / "Servicio no disponible
  temporalmente" / "Otro" + free text) — this was proposed but not explicitly re-confirmed after
  the rejection-reason discussion; flagged here so it's easy to correct if wrong.

## See also

- `docs/domain/provider-flow.md` — full event list, command/actor/aggregate breakdown, and BDD
  scenarios for the Provider Management + Scheduling contexts (the Provider's side of the app)
- `docs/domain/customer-flow.md` — same, for the Customer side (search, discovery, address, the
  Customer's half of the Booking lifecycle)
