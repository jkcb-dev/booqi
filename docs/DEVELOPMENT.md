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
To run several roles at once, give each its own worktree (see "Parallel work in git worktrees").

## Ticket flow and dependencies

- One branch per ticket (`feature/<issue>-<slug>`), PR into `main`, never merge your own PR.
- **Dependency state — check, don't assume.** Before branching, run `gh pr list` and
  `git log origin/main`. If the ticket needs *compile-time* types from an unmerged PR, branch from
  that PR's branch and open the PR with that branch as base (say so, and note "retarget to `main`
  once the base merges"). If it only needs an ID/String reference (e.g. `Service.providerId`), it
  has no compile-time dependency: branch from `main`.
- **`Closes #N` only fires when the PR merges into `main`.** A stacked PR (base ≠ `main`) won't
  close its issue; close it by hand once the work is on `main`.
- **Prefer not stacking at all** — wait for the base PR to merge, or put a small follow-up (like an
  Architect wiring commit) on the *same* PR. Stacking bit us twice: (1) if the base PR is
  squash-merged first, merging the stacked PR sends it into the dead base branch and it never
  reaches `main` (#45 → re-done as #46) — **retarget a stacked PR to `main` before merging it**;
  (2) merging two dependent PRs in the wrong order broke `main` (#47 before #48 → hotfix #49) —
  merge in dependency order.
- **Close keywords are greedy.** Any `Closes/Fixes #N` in a PR body closes `#N`, even in a sentence
  like "doesn't close #50" — write epic references without a close keyword (#57 closed epic #50).
- Feature wiring is a separate fast-follow PR owned by Architect (see `docs/ARCHITECTURE.md`
  § Wiring a feature in). A `role:compose-ui` PR lists what Architect must add under a
  "For Architect" heading and does not edit those files.
- Doc/process-only changes (`docs/`, `.claude/agents/`, `README.md`) go straight to `main`; source
  code goes through a PR.

## Parallel work in git worktrees

Several tickets can run at once, **one agent per git worktree** — never two agents in the same
checkout (they switch branches under each other). With the Agent tool, pass
`isolation: "worktree"`; by hand: `git worktree add ../Booqi-wt/<issue> -b feature/<issue>-<slug> origin/main`.

- **Pick a batch that doesn't touch the same files.** Fine: append-only registration lines
  (`domainModule`, `dataModule`, `providerModule`) and different docs sections. Not fine: two
  tickets on the same screen/feature files, or two tickets both editing `App.kt` / `Destination.kt`
  / `SampleData.kt` — those go sequentially (Architect wiring always happens after its UI ticket).
- **Every worktree branches from `origin/main` and its PR targets `main`** — parallel tickets never
  stack on each other. If B needs A's types, B isn't parallel with A.
- **Fresh worktree setup:** copy `local.properties` from the main checkout (it is gitignored and
  holds `sdk.dir`; without it the Android build fails). The first Gradle build in a new worktree is
  slower (its own `build/` dirs; the `~/.gradle` cache is shared). graphify's git hooks fire in every
  worktree — harmless (`graphify-out/` is gitignored).
- **Capacity:** this machine (12 cores, 36 GB) handles ~3 parallel Gradle builds. Kotlin/Native
  (iOS) compiles are the heaviest — run the iOS test tasks once, at the end of a ticket.
- **Merging a batch:** merge one PR at a time. After each merge, rebase every remaining PR on
  `origin/main` (`git rebase origin/main`; registration-line conflicts → keep both sides), rerun its
  build + tests, and push with `--force-with-lease`. Only then merge the next one.
- **A clean textual rebase is not enough — compile the test sources too.** Parallel tickets break
  each other *semantically*: when one adds a method to a repository interface (e.g. #22's
  `UserRepository.saveAddress`), the hand-written test fakes another ticket wrote against the old
  interface stop compiling (#58's three `FakeUserRepository`s). After every rebase run the affected
  modules' `testAndroidHostTest` (which compiles tests), not just `assembleDebug`.
- **Cleanup:** `git worktree remove <path>` after the PR merges; `git worktree prune` for leftovers.

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
- **Current user:** Identity exists (#56 domain & data, #58 UI): `ObtenerUsuarioActual` (null when
  browsing without an account), access guard `RequerirCuentaVerificada` before booking / Provider
  mode (`feature:browse` runs it on "Reservar", `feature:account` on "Convertirme en Proveedor",
  `feature:provider` before `ActivarModoProveedor`). `providerId` everywhere is the **ProviderProfile
  id** — in `feature:provider` always through `CurrentProvider` (wraps `ObtenerMiPerfilDeProveedor`,
  resolved on every use because ViewModels outlive a visit and the account can change), never the
  user id and never a constant. Don't add placeholders; use the use cases above.
- **Demo account (fake-only test credentials, no real accounts exist until Supabase Auth, #27):**
  email `demo@booqi.test`, password `Demo1234`. Seeded by `SampleData` (see its KDoc): verified, owns
  the demo Provider profile "Demo Studio" with two services, sample bookings (one pending, two
  confirmed, two completed with ratings) and sample customers ("Camila Torres", one deleted account →
  "Usuario eliminado"). Its active Provider bookings make "Eliminar cuenta" answer with the blocking
  list. A new email sign-up starts *unverified* (use "Ya verifiqué mi correo" on the verification
  notice — the fake confirms at once); a social button creates a verified account. The seed lives
  only in memory and starts signed out on every launch.
- **Date/time: use `kotlin.time.Clock` / `kotlin.time.Instant`, with `kotlinx-datetime` pinned to
  the version Material3 resolves (currently 0.7.1).** Material3 drags its own `kotlinx-datetime`
  onto the iOS classpath of `:shared` and every feature module, so a different pin in the catalog
  silently gives `:domain` a klib compiled against classes that no longer exist at link time (in
  0.7.x `kotlinx.datetime.Clock/Instant` are typealiases). `compileKotlinIosSimulatorArm64` and
  `:domain`'s own iOS tests do **not** catch this — it only fails at runtime in the app and in
  feature-module iOS tests. After touching date/time or bumping that version, run
  `:feature:provider:iosSimulatorArm64Test`, not just the domain tests. (Found on #19.)
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
