package corp.khin.solutions.booqi.core.designsystem.theme

import androidx.compose.ui.graphics.Color

// Placeholder token values — replace with the real values pulled from the Booqi Figma file
// (foundations page) once that hand-off happens. Naming follows Material 3 role names on
// purpose so the swap is a find-and-replace, not a redesign of this file's shape.
//
// TODO(design-system, future phase): once real tokens exist, prefer overriding/constructing the
// Material3 ColorScheme object directly (colorScheme.copy(...) or a single token map) instead of
// this one-val-per-role layout — flagged by the user as unnecessary ceremony while there's no
// real design to back it yet.

val BooqiPrimaryLight = Color(0xFF3A5B8C)
val BooqiOnPrimaryLight = Color(0xFFFFFFFF)
val BooqiSecondaryLight = Color(0xFFE8A33D)
val BooqiOnSecondaryLight = Color(0xFF1A1A1A)
val BooqiBackgroundLight = Color(0xFFFFFBFE)
val BooqiOnBackgroundLight = Color(0xFF1C1B1F)
val BooqiSurfaceLight = Color(0xFFFFFBFE)
val BooqiOnSurfaceLight = Color(0xFF1C1B1F)
val BooqiErrorLight = Color(0xFFBA1A1A)

val BooqiPrimaryDark = Color(0xFFA9C6FF)
val BooqiOnPrimaryDark = Color(0xFF0A2E56)
val BooqiSecondaryDark = Color(0xFFF5C177)
val BooqiOnSecondaryDark = Color(0xFF2E1F00)
val BooqiBackgroundDark = Color(0xFF1C1B1F)
val BooqiOnBackgroundDark = Color(0xFFE6E1E5)
val BooqiSurfaceDark = Color(0xFF1C1B1F)
val BooqiOnSurfaceDark = Color(0xFFE6E1E5)
val BooqiErrorDark = Color(0xFFFFB4AB)

// Semantic colors for booking status — not part of Material's default role set, but needed
// across every feature that shows a booking, so they live in the shared design system.
val BooqiStatusConfirmed = Color(0xFF2E7D32)
val BooqiStatusPending = Color(0xFFE8A33D)
val BooqiStatusCancelled = Color(0xFFBA1A1A)
val BooqiStatusCompleted = Color(0xFF6B6B6B)
