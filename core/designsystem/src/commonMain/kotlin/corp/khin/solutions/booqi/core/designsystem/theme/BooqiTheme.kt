package corp.khin.solutions.booqi.core.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

@Composable
fun BooqiTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) {
        darkColorScheme(
            primary = BooqiPrimaryDark,
            onPrimary = BooqiOnPrimaryDark,
            secondary = BooqiSecondaryDark,
            onSecondary = BooqiOnSecondaryDark,
            background = BooqiBackgroundDark,
            onBackground = BooqiOnBackgroundDark,
            surface = BooqiSurfaceDark,
            onSurface = BooqiOnSurfaceDark,
            error = BooqiErrorDark,
        )
    } else {
        lightColorScheme(
            primary = BooqiPrimaryLight,
            onPrimary = BooqiOnPrimaryLight,
            secondary = BooqiSecondaryLight,
            onSecondary = BooqiOnSecondaryLight,
            background = BooqiBackgroundLight,
            onBackground = BooqiOnBackgroundLight,
            surface = BooqiSurfaceLight,
            onSurface = BooqiOnSurfaceLight,
            error = BooqiErrorLight,
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = booqiTypography(),
        content = content,
    )
}
