# Booqi Backlog

No external ticket system yet — this file is the backlog of record until one exists (see
`docs/adr` — not yet written — for that decision if/when it happens). It's versioned, so its
history *is* the project's task history.

**How to use this:**
- One feature = one section. One role = one checklist item within it.
- A role's box gets checked only when its slice actually builds/runs/tests pass — not when the
  code is merely written. Match the verification bar the scaffold itself was held to (real
  `./gradlew` runs, not self-report).
- "n/a this slice" means the role has nothing to do for this feature, not that it's optional.
- Whoever (human, or an agent — Cursor background agent, a Claude Code subagent, etc.) picks up a
  role's item should read only that item plus the shared `Destination`/interface contracts it
  depends on — not the other roles' in-progress work. That boundary is what makes parallel work
  safe; see the roles conversation in project history for why.
- Update this file in the same commit as the work it tracks.

---

## Feature: Browse
**Status: done** (scaffolded 2026-08-11, `f1908dd`)

- [x] Architect — module graph, Koin wiring, `Navigator`/`Destination`, detekt baseline
- [x] Domain & Data — `ServiceProvider`, `ServiceCatalogRepository`, `GetFeaturedProvidersUseCase`;
      backed by a **temporary in-memory fake datasource** — no real backend exists yet, replace
      before this ships
- [x] Compose UI — Browse MVI triad (`BrowseUiState`/`Action`/`Event`), `BrowseScreen`; design
      tokens are placeholders, not real Figma values yet
- [x] Platform Integration — Koin bootstrap on both `androidApp` and `iosApp`, verified via real
      Android + iOS Simulator builds

---

## Feature: Booking flow
**Status: not started — next up**

`Destination.Booking(providerId)` and `Destination.BookingConfirmation` already exist in
`core:navigation` from the Browse scaffold, so Architect has nothing new to add for this slice.

- [ ] Architect — n/a this slice (`Navigator`/`Destination` already cover it)
- [ ] Domain & Data
  - [ ] `Booking` domain model (provider, date, time slot, status)
  - [ ] `TimeSlot` model + an availability concept (still backed by a fake/in-memory datasource —
        no real backend yet, same caveat as Browse)
  - [ ] `BookingRepository` interface + impl — **remote-first, no local cache** (availability goes
        stale fast; this was the caching-policy call made during the architecture discussion)
  - [ ] `CreateBookingUseCase`, `GetAvailableSlotsUseCase`
- [ ] Compose UI
  - [ ] Booking MVI triad: date strip + time-slot grid selection, booking summary card
  - [ ] Wires to `ProviderCard`'s `onClick` in Browse (currently navigates to `ProviderDetail`,
        which doesn't have a screen yet either — see below)
- [ ] Platform Integration — n/a this slice unless a platform capability turns out to be needed
      (e.g. a native date picker) — evaluate when Compose UI's ticket is in progress, don't
      pre-build it speculatively

---

## Feature: Provider Detail
**Status: not started**

`Destination.ProviderDetail(providerId)` exists; Browse already navigates to it on tap. No
domain/data/UI work has started.

- [ ] Domain & Data — expose full provider profile (bio, service list) vs. the summary shape
      `ServiceProvider` currently has
- [ ] Compose UI — provider profile screen

---

## Feature: My Bookings
**Status: not started**

`Destination.MyBookings` exists as a nav target only.

- [ ] Domain & Data — `BookingsHistoryRepository`, **local-first with background sync** (the other
      caching-policy call from the architecture discussion — this data should work offline)
- [ ] Compose UI — bookings list with status badges (confirmed/pending/cancelled/completed)

---

## Cross-cutting / not tied to one feature
- [ ] Pull real design tokens from Figma, replace `core:designsystem`'s placeholder values
- [ ] Real backend decision — replaces every "temporary fake datasource" note above
- [ ] `core:database` gets its first real SQLDelight schema (currently just the driver-factory
      abstraction, no schema — see My Bookings, the first feature that actually needs local
      storage)
- [ ] Document the "fresh clone needs `graphify hook install`" gap somewhere (README or a setup
      script) — flagged when graphify was set up, not yet fixed
