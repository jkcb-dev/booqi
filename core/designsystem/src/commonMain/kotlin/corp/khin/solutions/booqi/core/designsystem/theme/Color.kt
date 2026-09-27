package corp.khin.solutions.booqi.core.designsystem.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Real values pulled from the Booqi Figma file (Foundations page, Color Tokens section) —
// resolves issue #7. Single source of truth for every hex value in the app: BooqiTheme maps
// these onto Material3 ColorScheme roles, and BooqiExtendedColors below carries the tokens that
// have no Material3 role equivalent.
object BooqiColorTokens {
    val Brand = Color(0xFF3A9B7A)
    val BrandLight = Color(0xFFE6F5F0)
    val BrandDark = Color(0xFF2A7A5F)
    val Accent = Color(0xFFF5825A)
    val AccentLight = Color(0xFFFEF1EB)
    val Surface = Color(0xFFFAFAF8)
    val Card = Color(0xFFFFFFFF)
    val Ink = Color(0xFF1A1A18)
    val Ink2 = Color(0xFF6B7070)
    val Ink3 = Color(0xFFA8ACAC)
    val Border = Color(0xFFE8E6E0)

    // Booking status colors (StatusBadgeES) — names match docs/DOMAIN.md's BookingStatus values.
    val StatusConfirmada = Color(0xFF3A9B7A)
    val StatusPendiente = Color(0xFFF0A030)
    val StatusRechazada = Color(0xFFE04E5A)
    val StatusCompletada = Color(0xFF5A72A0)
    val StatusExpirada = Color(0xFF9A6B4B)
    val StatusCanceladaProveedor = Color(0xFFC45C2B)
    val StatusCanceladaCliente = Color(0xFF9B3A6B)
}

/**
 * Tokens with no Material3 [androidx.compose.material3.ColorScheme] role. Read via
 * [LocalBooqiExtendedColors] (e.g. `LocalBooqiExtendedColors.current.border`) instead of
 * importing [BooqiColorTokens] directly in feature code, so theme/dark-mode swaps only touch
 * this file.
 */
data class BooqiExtendedColors(
    val ink2: Color,
    val ink3: Color,
    val border: Color,
    val statusConfirmada: Color,
    val statusPendiente: Color,
    val statusRechazada: Color,
    val statusCompletada: Color,
    val statusExpirada: Color,
    val statusCanceladaProveedor: Color,
    val statusCanceladaCliente: Color,
)

val BooqiLightExtendedColors = BooqiExtendedColors(
    ink2 = BooqiColorTokens.Ink2,
    ink3 = BooqiColorTokens.Ink3,
    border = BooqiColorTokens.Border,
    statusConfirmada = BooqiColorTokens.StatusConfirmada,
    statusPendiente = BooqiColorTokens.StatusPendiente,
    statusRechazada = BooqiColorTokens.StatusRechazada,
    statusCompletada = BooqiColorTokens.StatusCompletada,
    statusExpirada = BooqiColorTokens.StatusExpirada,
    statusCanceladaProveedor = BooqiColorTokens.StatusCanceladaProveedor,
    statusCanceladaCliente = BooqiColorTokens.StatusCanceladaCliente,
)

// The Figma file's Foundations page defines only one (light) palette — this reuses those same
// tokens rather than inventing dark-mode hex values that don't trace back to Figma. Revisit once
// a real dark palette exists there.
val BooqiDarkExtendedColors = BooqiLightExtendedColors

val LocalBooqiExtendedColors = staticCompositionLocalOf { BooqiLightExtendedColors }
