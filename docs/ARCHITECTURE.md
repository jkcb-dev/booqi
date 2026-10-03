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
