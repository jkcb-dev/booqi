# Design System — Atomic Design

Organizes everything confirmed on the Booqi Figma file's **Foundations** and **Components** pages
(re-verified 2026-09-27, real values extracted directly from the Figma Make preview — not
estimated) using [Atomic Design](https://atomicdesign.bradfrost.com/) (Brad Frost): Atoms →
Molecules → Organisms → Templates → Pages. Pairs with [`SCREENS.md`](SCREENS.md), which maps
Pages to their GitHub tickets; this doc maps everything *below* Pages to its code home, resolving
issue #7 (real design tokens replacing `core:designsystem`'s placeholders).

## Design tokens (atoms — foundational values, not composables)

Implemented in `core:designsystem/theme/{Color,Type,Dimens}.kt`.

### Color

| Token | Hex | Token | Hex |
|---|---|---|---|
| Brand | `#3A9B7A` | Ink | `#1A1A18` |
| Brand Light | `#E6F5F0` | Ink 2° | `#6B7070` |
| Brand Dark | `#2A7A5F` | Ink 3° | `#A8ACAC` |
| Accent | `#F5825A` | Border | `#E8E6E0` |
| Accent Light | `#FEF1EB` | Surface | `#FAFAF8` |
| Card | `#FFFFFF` | | |

Status colors (`StatusBadgeES`, matches `docs/DOMAIN.md`'s `BookingStatus`):

| Estado | Hex |
|---|---|
| Confirmada | `#3A9B7A` |
| Pendiente | `#F0A030` |
| Rechazada | `#E04E5A` |
| Completada | `#5A72A0` |
| Expirada | `#9A6B4B` |
| Canc. Prov. | `#C45C2B` |
| Canc. Cli. | `#9B3A6B` |

Only a light palette exists in Figma today — `BooqiTheme`'s dark scheme is a mechanical M3
derivation of the same hues, flagged in code, not a second Figma-sourced set.

### Type scale — Nunito

| Style | Size · Weight |
|---|---|
| Display | 32px · 800 (ExtraBold) |
| Title | 22px · 800 (ExtraBold) |
| Heading | 18px · 700 (Bold) |
| Body | 15px · 400 (Regular) |
| Label | 13px · 700 (Bold) |
| Caption | 11px · 500 (Medium) |

Font family is Compose's default pending a follow-up ticket to add Nunito's `.ttf` files as a
`composeResources` font — sizes/weights are already Figma-accurate.

### Spacing — 8pt scale

`4 · 8 · 12 · 16 · 24 · 32 · 48 · 64`

### Corner radius

| Token | Value |
|---|---|
| Small | 10px |
| Medium | 16px |
| Large | 24px |
| Pill | 999px |

## Atoms (composables)

Single-purpose, no internal composition of other components.

| Component | Variants (from Figma) |
|---|---|
| Button | Primary / Secondary / Destructive, each Default/Pressed/Disabled |
| Input field | Default / Focus / Error |
| StatusBadgeES | 7 estados (see status colors above) |
| Category chip | Todos/Barber/Uñas/Limpieza/Masajes/Técnico (Browse categories) |
| Modality badge | Local / Domicilio / Local & Dom. |

## Molecules

Small groups of atoms functioning as one unit.

| Component | Composition |
|---|---|
| Rating summary | star row + numeric score + review count |
| Schedule day row | day label + enable toggle + time range (used inside `WeeklyScheduleEditor`) |
| Time slot cell | single bookable/unavailable time button (used inside `TimeSlotGrid`) |
| Reason option | radio + label (used inside `ReasonPicker`) |
| Empty state body | icon + title + description + CTA button |

## Organisms

Complete, self-sufficient UI sections combining molecules/atoms.

| Component | Notes |
|---|---|
| `ProviderCard` | photo, name, especialidad, rating, distancia, precio — Browse results |
| `WeeklyScheduleEditor` | 7 schedule day rows |
| `DateBlockingCalendar` | monthly grid + blocked-date legend |
| `TimeSlotGrid` | grid of time slot cells, unavailable ones struck through |
| `ReasonPicker` | list of reason options + confirm button (interactive, Provider-side reject) |
| `RatingDisplay` | rating summary + histogram + individual reviews |
| `AddressPicker` | search field + map + confirm button |
| `EmptyState` | 4 confirmed variants: sin servicios, sin solicitudes, sin reservas, sin resultados |
| `BottomNavigation` | 2 variants: Cliente (Inicio/Buscar/Reservas/Perfil), Proveedor (Inicio/Solicitudes/Servicios/Perfil) |

## Templates

Per-session layout shells (top bar/content/bottom nav arrangement) — not yet built; each lands
with the first screen in its owning `feature:*` module, per `docs/ARCHITECTURE.md`'s three-module
policy (`feature:browse`, `feature:booking`, `feature:provider`).

## Pages

The 23 confirmed screens (P1–P11 Proveedor, C1–C12 Cliente) — see [`SCREENS.md`](SCREENS.md) for
the per-ticket mapping and confirmed structure.

## Module ownership

Atoms and the design-token layer always live in `core:designsystem` — that's not in question.
Molecules/organisms follow the same rule that governs everything else: **if more than one
`feature:*` module needs it, it can't live inside just one of them** (no `feature:*` → `feature:*`
imports), so it moves up to `core:designsystem`.

| Layer | Component | Owner | Why |
|---|---|---|---|
| Atom | Button, Input field, StatusBadgeES, Category chip, Modality badge | `core:designsystem` | used across every feature module |
| Organism | `EmptyState` | `core:designsystem` | all three feature modules need an empty state |
| Organism | `BottomNavigation` | `core:designsystem` | both Cliente and Proveedor session shells need it |
| Organism | `RatingDisplay` | `core:designsystem` | shown in both `feature:browse` (C4) and `feature:provider` (P11) |
| Organism | `ReasonPicker` | `core:designsystem` | Provider picks the reason (P9), Customer sees it read-only (C10) — cross-module |
| Organism | `ProviderCard` | `feature:browse` | Browse-results-only (C2) |
| Organism | `WeeklyScheduleEditor`, `DateBlockingCalendar` | `feature:provider` | Provider-schedule-only (P6/P7) |
| Organism | `TimeSlotGrid` | `feature:booking` | Customer slot selection only (C5) |
| Organism | `AddressPicker` | `feature:booking` | Customer address only (C6) — see issues #23/#24 |

If a future ticket needs one of the `feature:*`-owned organisms from a second feature module,
that's the trigger to promote it into `core:designsystem` — don't duplicate it.

## Known gaps

- Dark color palette: not defined in Figma yet; current dark scheme is a placeholder derivation
  (see `BooqiTheme.kt` comments).
- Nunito font files: sizes/weights are wired, actual `.ttf` resources are a follow-up ticket.
- Templates: intentionally not built ahead of the screens that need them (no speculative
  building, per every role file's rules).
