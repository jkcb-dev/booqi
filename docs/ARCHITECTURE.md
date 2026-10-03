# Booqi Architecture Reference

Canonical home for structural decisions that don't belong in `docs/DOMAIN.md` (that's the product
model) — this is the codebase's shape. Referenced by `.claude/agents/booqi-architect.md` and
`.claude/agents/booqi-compose-ui.md` rather than duplicated there, so it only needs updating once.

## Module graph

```
feature:browse, feature:booking, feature:provider   ← Compose UI owns these
              ↓
            domain                                  ← Shared Domain & Data owns these
              ↓
             data
              ↓
core:common, core:network, core:database, core:designsystem, core:navigation
```

One-way dependencies only. `core:*` never depends upward. `domain` never imports Compose or Ktor.
`feature:*` modules never import each other directly — they navigate via the injected `Navigator`
(`core:navigation`), referencing `Destination` by its sealed type.

## Feature module policy (decided 2026-08-12)

**Three `feature:*` modules, grouped by who uses them in one continuous session — not one module
per screen.** A 1:1 screen-to-module mapping doesn't scale past a handful of screens; grouping by
user session keeps the module count sane as more tickets land.

| Module | Screens | Why grouped here |
|---|---|---|
| `feature:browse` (exists) | Search, results, service detail, provider profile view | The Catalog/discovery part of a Customer session |
| `feature:booking` (new) | Time-slot selection, confirm, pending, confirmed, rejected/expired, cancel, rate, history, address picker | The Scheduling part of a Customer session — address lives here because it's only ever touched mid-booking |
| `feature:provider` (new) | Activate mode, complete/pause profile, manage services, manage schedule, booking request inbox, accept/reject/cancel | The entire Provider-side experience — one persona, one module |

**Rule for adding a new feature module** (not just adding a screen to an existing one): only when
a new group of screens serves a genuinely different user session/persona than the three above —
not per individual screen, not per bounded context, not per GitHub issue. If unsure whether
something is a new module or belongs in an existing one, default to extending an existing module;
splitting later is cheap, over-splitting early isn't.

## Cross-cutting file ownership (avoids collisions as ticket count grows)

These files get touched by multiple tickets over time. Ownership stays with **Architect**, even
though other roles' tickets will often need something added to them:

- `core:navigation/Destination.kt` — new destinations get added here as features land. A
  `role:domain-data` or `role:compose-ui` ticket that needs a new destination should note it in
  its PR description rather than editing this file directly; Architect adds it.
- `shared/.../di/InitKoin.kt` — the list of Koin modules registered at startup. Same pattern: new
  feature/domain modules get their Koin module added here by Architect, not by whichever ticket
  introduced them, to avoid two tickets racing on the same file.

This is a deliberate chokepoint, not bureaucracy for its own sake — it's the one place collisions
between parallel tickets would otherwise happen silently.

## Wiring a feature in (who does what)

Landing a feature touches several files with different owners. The split, settled while shipping
#13/#14:

| Step | File(s) | Owner |
|---|---|---|
| Create a **pre-approved** module scaffold (one of the three in the policy table above) | `settings.gradle.kts` `include(...)` + `feature/<x>/build.gradle.kts`, copied from `feature:browse` with only the `namespace` changed | `role:compose-ui`, inside its first ticket for that module. Creating a module *not* in the policy table is Architect's call. |
| Screens, ViewModel, `<x>Module` Koin module | `feature/<x>/**` | `role:compose-ui` |
| New `Destination` entries | `core:navigation/Destination.kt` | Architect |
| Register the Koin module | `shared/.../di/InitKoin.kt` | Architect |
| Module dependency + `when` branch that renders the screen | `shared/build.gradle.kts`, `shared/.../App.kt` | Architect |

So a feature ships as **two PRs**: the `role:compose-ui` PR (ends with a "For Architect" section
listing the exact lines needed), then an Architect **wiring PR** stacked on or following it. The
wiring PR is also where the feature is first reachable, so it's where it gets **run on a simulator
and screenshotted** (see `docs/DEVELOPMENT.md`). A compose-ui PR alone compiles but shows nothing
new in the running app — that is expected, not a bug.

Until a real entry point exists (Identity/profile screens aren't built), the wiring PR may add a
clearly-commented TEMPORARY navigation affordance in `App.kt` so the flow is reachable; it is
removed when the real trigger lands.

## Booking expiry trigger (decision input for #27 / platform work)

A `Requested` Booking must become `Expired` once `requestedAt + 24h <= now` (provider-flow.md
§ Grupo 4). The **rule** is built and tested in `domain` (`Booking.expire`,
`ExpirarSolicitudesVencidasUseCase`, idempotent, takes a `Clock`); what is deliberately **not**
built is *what calls it*. Realistic options:

| Option | How | For | Against |
|---|---|---|---|
| **A. Server-side schedule (Supabase `pg_cron`, or a scheduled Edge Function)** | Every few minutes run the same rule on the server: `UPDATE bookings SET status = 'Expired' WHERE status = 'Requested' AND requested_at + interval '24 hours' <= now()` | Fires with no app open (needed for the Customer's expiry message and any future push); one writer, so no RLS problem (a Provider's device can't update other Providers' rows); no dependence on iOS background rules | The 24h rule lives twice (Kotlin + SQL) — keep the SQL a one-liner mirroring `Booking.RESPONSE_WINDOW` and pin the equivalence with a test in #27 |
| **B. Periodic client call** (WorkManager on Android, `BGTaskScheduler` on iOS) calling `ExpirarSolicitudesVencidasUseCase` | The app wakes up and sweeps | Reuses the Kotlin rule verbatim | Unreliable: iOS background tasks are opportunistic, Android defers under Doze, nothing runs if the user never opens the app; the sweep touches *all* Providers' rows, which RLS (and least privilege) should not allow from a client |
| **C. Lazy / on-read** | Treat an overdue `Requested` as expired whenever it is read or acted on | Needs no scheduler at all | Already partly done in the domain (`isResponseOverdue`: accept/reject refuse a lapsed request and the inbox hides it), but a Customer's "Pendiente" stays stale and the TimeSlot stays held until someone reads, and no event ever fires |

**Recommendation:** A as the authority (Supabase `pg_cron`, every ~5 min), with C's cheap guards
kept in the domain as defence-in-depth (they already are). Reserve B only if the backend ends up
without scheduled jobs. Implementation, the SQL, and the equivalence test belong to #27 (Supabase
schema) / the platform work, not to the domain tickets.
