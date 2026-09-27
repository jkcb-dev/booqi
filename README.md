# Booqi

A Kotlin Multiplatform (Android + iOS, Compose Multiplatform UI) two-sided marketplace app: any
User can book services (customer) and optionally also offer them (Provider) — see
[`docs/DOMAIN.md`](docs/DOMAIN.md) for the full ubiquitous-language glossary and domain model.

## Start here

- [`docs/DOMAIN.md`](docs/DOMAIN.md) — the product model (DDD): entities, aggregate rules, bounded
  contexts. Read this before touching `domain`/`data`.
- [`docs/domain/provider-flow.md`](docs/domain/provider-flow.md) /
  [`docs/domain/customer-flow.md`](docs/domain/customer-flow.md) — the event list + Gherkin BDD
  scenarios per feature group.
- [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) — the module graph, the `feature:*` consolidation
  policy, and cross-cutting file ownership.
- [`docs/DATABASE.md`](docs/DATABASE.md) — the Supabase/Postgres schema (3NF ER diagram).
- [`docs/design/SCREENS.md`](docs/design/SCREENS.md) /
  [`docs/design/DESIGN_SYSTEM.md`](docs/design/DESIGN_SYSTEM.md) — the Figma screen-to-ticket
  mapping and the Atomic Design token/component catalog.

Work is tracked as [GitHub Issues](https://github.com/jkcb-dev/booqi/issues), labeled by role
(`role:architect`/`role:domain-data`/`role:compose-ui`/`role:platform-integration`) and context
(`context:provider`/`context:customer`/`cross-cutting`). Each role's responsibilities, ownership,
and workflow are defined in [`.claude/agents/`](.claude/agents/). Branching follows GitHub Flow:
one `feature/<issue-number>-<slug>` branch per ticket, PR into `main`, no direct commits to `main`
except for meta/process files (docs, `.claude/agents/`).

## Module structure

```
feature:browse                                      ← Compose UI: Catalog/discovery (Customer)
              ↓
            domain                                   ← Shared Domain & Data: entities, use cases
              ↓
             data                                    ← Shared Domain & Data: repositories, datasources
              ↓
core:common, core:network, core:database,
core:designsystem, core:navigation                   ← Architect
```

`feature:booking` and `feature:provider` are planned modules (not yet created — see
`docs/ARCHITECTURE.md`'s feature-module policy) that land once their first ticket needs them.
`androidApp`/`iosApp`/`shared` are the platform entry points (Platform Integration).

## Setup for a fresh clone

This repo uses graphify for an auto-rebuilding knowledge graph on every commit. Git hooks aren't
versioned by git itself, so run once after cloning:

```bash
graphify hook install
```

## Running the apps

- Android app: `./gradlew :androidApp:assembleDebug`
- iOS app: open [`/iosApp`](./iosApp) in Xcode and run it from there, or use the IDE run widget.

## Running tests

- Android tests: `./gradlew :shared:testAndroidHostTest`
- iOS tests: `./gradlew :shared:iosSimulatorArm64Test`
- Static analysis: `./gradlew detekt`

---

Built with [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html)
and [Compose Multiplatform](https://www.jetbrains.com/lp/compose-multiplatform/).
