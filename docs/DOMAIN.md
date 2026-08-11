# Booqi Domain Model (DDD)

Written before the Booking domain model exists in code, on purpose — so `Booking` gets designed
correctly the first time instead of refactored later. This is the ubiquitous language: use these
exact terms in code, tickets, and conversation. If a term drifts from this doc, fix the doc, not
the other way around.

Scope note: this is **tactical DDD** (ubiquitous language, aggregates, value objects, invariants),
not full strategic DDD. Bounded contexts below stay as clearly-named packages inside the existing
`domain`/`data`/`feature:*` modules for now, not separate Gradle modules — that split is only
worth the ceremony once a context's team/release cadence actually diverges from the others, which
hasn't happened for a project this size. Revisit if that changes.

## Ubiquitous language

| Term | Meaning |
|---|---|
| **Customer** | The app user browsing and booking services. Not modeled yet — no auth built. |
| **Provider** | A business/individual offering bookable services (nails, barber, technician, ...). Code: `ServiceProvider`. |
| **Service** | A specific offering a Provider provides, with its own price. Currently folded into `ServiceProvider.category`/`priceFromCents` — a known simplification, see Open Questions. |
| **TimeSlot** | A specific bookable unit of time for a Provider. Value object, not an entity — equality is by value (`providerId` + date + start time), it has no identity of its own. |
| **Availability** | The set of TimeSlots a Provider currently offers as bookable. |
| **Booking** | A Customer's reservation of a Provider's TimeSlot. Aggregate root for the Scheduling context. |
| **Booking Status** | Lifecycle state of a Booking: `Requested → Confirmed → Completed`, or `→ Cancelled` from either of the first two. No other transitions are valid. |

## Bounded contexts

**Catalog** — browsing and discovery. Owns `ServiceProvider`. Maps to `feature:browse` +
`domain`'s current `ServiceProvider`/`ServiceCatalogRepository`. Already built.

**Scheduling** — the booking lifecycle. Owns `Booking`, `TimeSlot`, `Availability`, status
transitions. Not built yet — this is the Booking flow ticket (Issues #1, #2).

**Identity** and **Notifications** — not needed yet (no auth, no push/email). Named here so future
work has a home to land in without inventing new vocabulary later.

## Aggregate boundaries — the rule that matters

**`Booking` references `Provider` by ID (`providerId: String`), never by embedding a
`ServiceProvider` object.** This is the one DDD rule most worth enforcing here: Catalog and
Scheduling are different aggregates with different lifecycles (a Provider's rating changes
independently of any Booking referencing it), so Booking must not hold a live/stale copy of
Provider data. If a screen needs both, it fetches each through its own repository and composes
them at the presentation layer — never by denormalizing Provider fields onto Booking.

```
Booking (aggregate root)
├─ id: String
├─ providerId: String          ← reference, not embedded ServiceProvider
├─ timeSlot: TimeSlot          ← value object, embedded (owned by this aggregate)
├─ status: BookingStatus
└─ customerId: String?         ← nullable until Identity exists
```

`BookingStatus` values (`Requested`, `Confirmed`, `Completed`, `Cancelled`) are also the vocabulary
already baked into the design system's status-badge colors
(`BooqiStatusConfirmed`/`Pending`/`Cancelled`/`Completed` in `core:designsystem/theme/Color.kt`) —
that predates this doc but happens to already agree with it, which is a good sign, not a
coincidence to paper over.

## Open questions (deliberately deferred, not forgotten)

- **Service vs. ServiceProvider**: a Provider likely offers multiple Services at different prices
  (e.g. "gel manicure" vs "acrylic fill"), which the current single-category-single-price model
  can't express. Resolve this when the Provider Detail ticket is picked up — its issue already
  says "expose full provider profile... service list," which is where this gets modeled properly.
- **Identity**: `Booking.customerId` is nullable until auth exists. Don't backfill a fake customer
  concept just to make it non-nullable — the nullability is honest about what's actually known
  right now.
