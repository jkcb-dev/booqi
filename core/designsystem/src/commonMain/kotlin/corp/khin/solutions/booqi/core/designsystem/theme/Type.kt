package corp.khin.solutions.booqi.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Real type scale from the Booqi Figma file (Foundations page, Type Scale · Nunito section) —
// resolves issue #7. Sizes/weights below match Figma exactly; the Nunito family itself comes from
// booqiFontFamily() (Font.kt, issue #31) and is applied to every style in booqiTypography().
//
// Figma's 6-step scale (Display/Title/Heading/Body/Label/Caption) is coarser than Material3's 15
// roles, so each step covers the M3 roles closest to it rather than inventing extra sizes.
private object BooqiTypeScale {
    val Display = TextStyle(fontSize = 32.sp, fontWeight = FontWeight.ExtraBold)
    val Title = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
    val Heading = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold)
    val Body = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Normal)
    val Label = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Bold)
    val Caption = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Medium)
}

@Composable
fun booqiTypography(): Typography {
    val family = booqiFontFamily()
    val display = BooqiTypeScale.Display.copy(fontFamily = family)
    val title = BooqiTypeScale.Title.copy(fontFamily = family)
    val heading = BooqiTypeScale.Heading.copy(fontFamily = family)
    val body = BooqiTypeScale.Body.copy(fontFamily = family)
    val label = BooqiTypeScale.Label.copy(fontFamily = family)
    val caption = BooqiTypeScale.Caption.copy(fontFamily = family)
    return Typography(
        displayLarge = display,
        displayMedium = display,
        displaySmall = display,
        headlineLarge = display,
        headlineMedium = title,
        headlineSmall = title,
        titleLarge = title,
        titleMedium = heading,
        titleSmall = heading,
        bodyLarge = body,
        bodyMedium = body,
        bodySmall = caption,
        labelLarge = label,
        labelMedium = label,
        labelSmall = caption,
    )
}
