package corp.khin.solutions.booqi.core.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

// Built directly from BooqiColorTokens (Figma Foundations values) rather than one Color val per
// M3 role — see issue #7: with a real palette in hand, mapping the token object onto
// ColorScheme roles is the natural shape instead of ceremony.
private val BooqiLightColorScheme = lightColorScheme(
    primary = BooqiColorTokens.Brand,
    onPrimary = BooqiColorTokens.Card,
    primaryContainer = BooqiColorTokens.BrandLight,
    onPrimaryContainer = BooqiColorTokens.BrandDark,
    secondary = BooqiColorTokens.Accent,
    onSecondary = BooqiColorTokens.Card,
    secondaryContainer = BooqiColorTokens.AccentLight,
    onSecondaryContainer = BooqiColorTokens.Accent,
    background = BooqiColorTokens.Surface,
    onBackground = BooqiColorTokens.Ink,
    surface = BooqiColorTokens.Card,
    onSurface = BooqiColorTokens.Ink,
    outline = BooqiColorTokens.Border,
    error = BooqiColorTokens.StatusRechazada,
    onError = BooqiColorTokens.Card,
)

// Figma's Foundations page defines only the light palette above — this dark scheme is a
// mechanical M3 dark derivation of the same brand/accent hues, not a Figma-sourced token set.
// Swap it out once a real dark palette lands there.
private val BooqiDarkColorScheme = darkColorScheme(
    primary = BooqiColorTokens.BrandLight,
    onPrimary = BooqiColorTokens.BrandDark,
    secondary = BooqiColorTokens.AccentLight,
    onSecondary = BooqiColorTokens.Accent,
    background = BooqiColorTokens.Ink,
    onBackground = BooqiColorTokens.Surface,
    surface = BooqiColorTokens.Ink,
    onSurface = BooqiColorTokens.Surface,
    outline = BooqiColorTokens.Ink2,
    error = BooqiColorTokens.StatusRechazada,
    onError = BooqiColorTokens.Card,
)

@Composable
fun BooqiTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) BooqiDarkColorScheme else BooqiLightColorScheme
    val extendedColors = if (darkTheme) BooqiDarkExtendedColors else BooqiLightExtendedColors

    CompositionLocalProvider(LocalBooqiExtendedColors provides extendedColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = booqiTypography(),
            content = content,
        )
    }
}
