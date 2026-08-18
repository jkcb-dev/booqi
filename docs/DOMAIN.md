# Booqi Domain Model (DDD)

**Corrected 2026-08-11** — the original version of this document conflated Provider and Service
into one entity. A product discovery session (see `docs/domain/provider-flow.md`) clarified that
they're separate concepts with separate lifecycles. This version replaces that one; nothing below
should be assumed to match the initial scaffold's `ServiceProvider` type, which is now known to be
wrong and needs splitting (tracked as follow-up work, not yet done).

Scope note, unchanged from before: this is **tactical DDD** (ubiquitous language, aggregates,
value objects, invariants), not full strategic DDD. Bounded contexts stay as packages inside the
existing modules, not separate Gradle modules, unless a context's pace of change actually diverges
enough to justify the ceremony.

## Ubiquitous language

| Term | Meaning |
|---|---|
| **User** | An account. Authenticates via Google, Facebook, Apple ID, or email/password. Any User can book services (customer capability is implicit); a User optionally also has a **ProviderProfile** if they choose to offer services. Being a customer and a provider is not an exclusive choice — one account can be both. |
| **ProviderProfile** | The identity of *who* offers services: display name, photo, description, location, aggregate rating, weekly availability. One optional ProviderProfile per User. |
| **Service** | The *what* — a specific offering a Provider provides, with its own title, photo, description, price, duration, and modality (Local / Domicilio / both). A ProviderProfile owns one or more Services. **A Provider is not a Service — this was the original modeling mistake.** |
| **Modality** | Whether a Service is delivered at the Provider's location (**Local**) or the Customer's (**Domicilio**), or both. |
| **TimeSlot** | A specific bookable unit of time for a Provider. Value object — equality by value (`providerId` + date + start time), no identity of its own. |
| **Availability** | A Provider's recurring weekly schedule, plus specific blocked dates/times, plus an optional "paused" date range (vacation mode). TimeSlots are generated from this. |
| **Booking** | A Customer's request to reserve a Provider's TimeSlot for a specific Service. Aggregate root for the Scheduling context. Goes through a request→accept/reject lifecycle — see `docs/domain/provider-flow.md` for the full state machine. |
| **BookingStatus** | `Requested → Confirmed → Completed`, or `Requested → Rejected` / `Requested → Expired` (24h no response), or `Confirmed → CancelledByProvider`. No other transitions are valid. |

## Bounded contexts

**Identity** — the `User` account itself: authentication, whether a ProviderProfile exists for
this user. Not built yet.

**Provider Management** — a Provider's own "back office": profile, Services (create/edit/disable),
Availability (define/modify schedule, block dates, pause profile). Fully specified in
`docs/domain/provider-flow.md`. Not built in code yet.

**Catalog** — browsing/discovery, read-heavy. Searches across Services (not Providers directly),
filterable by type, zone/distance (GPS-based, simple radius — no polygon zones). Partially built:
`feature:browse` + `domain`'s `ServiceProvider` exist but use the pre-correction model and need
reworking to the Service/ProviderProfile split above.

**Scheduling** — the Booking lifecycle: request, accept/reject/expire, complete, cancel, rate.
Fully specified in `docs/domain/provider-flow.md`'s "Gestión de Reservas y Calificaciones" group.
Not built in code yet.

## Aggregate boundaries — the rule that matters

Unchanged from the original version of this doc: **`Booking` references `Service` and
`ProviderProfile` by ID, never by embedding them.** Different aggregates, different lifecycles — a
Service's price changing shouldn't retroactively change what a past Booking says the customer
agreed to pay.

```
Booking (aggregate root)
├─ id: String
├─ providerId: String      ← reference
├─ serviceId: String       ← reference
├─ customerId: String
├─ timeSlot: TimeSlot       ← value object, embedded
├─ status: BookingStatus
├─ rejectionReason / cancellationReason: Reason?  ← predefined options + optional free text
└─ rating: Rating?          ← stars + optional comment, set only once status = Completed
```

`Rating` is modeled as an optional field on `Booking` itself, not a separate aggregate — a rating
is 1:1 with a completed Booking and has no independent lifecycle of its own. A Provider's overall
rating shown on their profile is a *computed aggregate* (average across all their Bookings'
ratings), not a stored field that gets manually updated.

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
- `docs/domain/customer-flow.md` — not written yet (Customer-side flow, next up)
