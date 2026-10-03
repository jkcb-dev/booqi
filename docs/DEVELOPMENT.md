# Development Playbook

Shared operational rules every role in `.claude/agents/` follows. Product rules live in
`docs/DOMAIN.md`, structure in `docs/ARCHITECTURE.md` — this file is the "how we actually work and
what has bitten us" reference, so the role files can point here instead of repeating it.

## Running a role

The `.claude/agents/booqi-*.md` files are **not** auto-registered as `subagent_type` values
(verified: `Agent type 'booqi-domain-data' not found`). To run a role on a ticket, spawn a
`general-purpose` agent with `model: sonnet`, and paste into the prompt: (1) the role file's full
content, (2) the issue body, (3) the doc section it references, (4) the existing code patterns to
match. Brief it on state you have **verified** (see "Dependency state" below), not state you assume.

## Ticket flow and dependencies

- One branch per ticket (`feature/<issue>-<slug>`), PR into `main`, never merge your own PR.
- **Dependency state — check, don't assume.** Before branching, run `gh pr list` and
  `git log origin/main`. If the ticket needs *compile-time* types from an unmerged PR, branch from
  that PR's branch and open the PR with that branch as base (say so, and note "retarget to `main`
  once the base merges"). If it only needs an ID/String reference (e.g. `Service.providerId`), it
  has no compile-time dependency: branch from `main`.
- **`Closes #N` only fires when the PR merges into `main`.** A stacked PR (base ≠ `main`) won't
  close its issue; close it by hand once the work is on `main`.
- Feature wiring is a separate fast-follow PR owned by Architect (see `docs/ARCHITECTURE.md`
  § Wiring a feature in). A `role:compose-ui` PR lists what Architect must add under a
  "For Architect" heading and does not edit those files.
- Doc/process-only changes (`docs/`, `.claude/agents/`, `README.md`) go straight to `main`; source
  code goes through a PR.

## Verification (a real run, not a self-report)

| Change | Minimum |
|---|---|
| domain / data | `./gradlew :domain:build :data:build detekt` + a test per BDD scenario |
| feature UI | `./gradlew :feature:<x>:build :androidApp:assembleDebug :shared:compileKotlinIosSimulatorArm64 detekt`, plus `grep -rnE 'Color\(0x\|[0-9]\.(dp\|sp)\b' feature/<x>/src` returns nothing |
| wiring (Architect) / anything visible | Run it on the iOS Simulator and screenshot the real screen — see below |

Running on the iOS Simulator:
```bash
xcrun simctl boot <device-udid>                # xcrun simctl list devices available
cd iosApp && xcodebuild -project iosApp.xcodeproj -scheme iosApp -configuration Debug \
  -sdk iphonesimulator -destination "id=<udid>" -derivedDataPath ./DerivedData build
# then launch iosApp/DerivedData/Build/Products/Debug-iphonesimulator/Booqi.app
```
`DerivedData/` is gitignored; delete it after verifying.

## Environment gotchas

- **Sandbox blocks Maven Central.** If Gradle fails resolving an already-declared dependency, retry
  the same command with `dangerouslyDisableSandbox: true`. Normal, not risky.
- **macOS has no `timeout`.** A command prefixed with it silently exits without running. Read the
  output of background builds; don't trust an exit code alone.
- **`gh` account.** This repo needs `jkcb-dev` active. Check `gh auth status` before pushing or
  opening a PR; fix with `gh auth switch --user jkcb-dev`. A 403 on push means the wrong account.
- **detekt `LongParameterList` (threshold 6).** Don't suppress it — group related arguments into a
  parameter object (e.g. `ServiceDetails`), which usually matches what a form submits anyway.
- **detekt function-count/length limits** apply to Composables: split a multi-step screen into
  one file per step (`...ActivateContent.kt`, `...CompleteContent.kt`).
- **Kotlin/Native test names**: no commas inside backticked test names — the iOS target rejects
  them even though the Android target accepts them.
- **No auth/session yet** (Identity context isn't built). Where a use case needs a user id, use a
  constant clearly marked `TEMPORARY` (see `ProviderProfileViewModel`) — don't build auth ahead of
  its ticket.
- **Validation failures** are `DomainError.InvalidInput(message)`, returned by the use case before
  any I/O. ViewModels surface them as form errors, never as a crash or a generic error event.
- **ViewModels are not destination-scoped.** Our simple `DefaultNavigator` + `when(backStack.last())`
  swaps composables without giving each destination its own `ViewModelStoreOwner`, so
  `koinViewModel()` hands the same instance back every time a screen re-enters composition (and
  Koin does not key it by `parametersOf(...)`; pass `key = ...` if you need separate instances).
  A screen must therefore **(re)load or reset on entry** — e.g. `LaunchedEffect(Unit) { onAction(Refresh) }`
  for a list, `LaunchedEffect(id) { onAction(Start(id)) }` for a form — and must not assume
  init-time loading is enough: "save in the editor, pop back to the list" otherwise shows the
  stale list, and "add → add again" shows the previous form. (Found on #15.)
