package corp.khin.solutions.booqi.core.designsystem.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import corp.khin.solutions.booqi.core.designsystem.resources.Res
import corp.khin.solutions.booqi.core.designsystem.resources.nunito_bold
import corp.khin.solutions.booqi.core.designsystem.resources.nunito_extrabold
import corp.khin.solutions.booqi.core.designsystem.resources.nunito_medium
import corp.khin.solutions.booqi.core.designsystem.resources.nunito_regular
import org.jetbrains.compose.resources.Font

/**
 * Booqi's brand typeface (Nunito, SIL OFL 1.1), covering exactly the four weights the Figma type
 * scale uses: Regular 400, Medium 500, Bold 700, ExtraBold 800 (static TTFs in
 * `composeResources/font`).
 *
 * This is the only place the font files are referenced. Features never touch `Res.font.*` — they
 * get Nunito for free through `MaterialTheme.typography` (wired in [booqiTypography]); anything
 * that builds its own `TextStyle` outside the theme should start from this family.
 *
 * `@Composable` because Compose Multiplatform loads resource fonts asynchronously inside the
 * composition; the family is remembered, so it is cheap to call repeatedly.
 */
@Composable
fun booqiFontFamily(): FontFamily {
    val regular = Font(Res.font.nunito_regular, FontWeight.Normal)
    val medium = Font(Res.font.nunito_medium, FontWeight.Medium)
    val bold = Font(Res.font.nunito_bold, FontWeight.Bold)
    val extraBold = Font(Res.font.nunito_extrabold, FontWeight.ExtraBold)
    return remember(regular, medium, bold, extraBold) {
        FontFamily(regular, medium, bold, extraBold)
    }
}
