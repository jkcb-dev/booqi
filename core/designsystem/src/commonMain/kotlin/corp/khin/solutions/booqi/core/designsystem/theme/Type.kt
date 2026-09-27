package corp.khin.solutions.booqi.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Real type scale from the Booqi Figma file (Foundations page, Type Scale · Nunito section) —
// resolves issue #7. Font family stays Compose's default until Nunito's .ttf files are added as
// a composeResources font in a follow-up ticket; sizes/weights below already match Figma exactly.
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
fun booqiTypography(): Typography = Typography(
    displayLarge = BooqiTypeScale.Display,
    displayMedium = BooqiTypeScale.Display,
    displaySmall = BooqiTypeScale.Display,
    headlineLarge = BooqiTypeScale.Display,
    headlineMedium = BooqiTypeScale.Title,
    headlineSmall = BooqiTypeScale.Title,
    titleLarge = BooqiTypeScale.Title,
    titleMedium = BooqiTypeScale.Heading,
    titleSmall = BooqiTypeScale.Heading,
    bodyLarge = BooqiTypeScale.Body,
    bodyMedium = BooqiTypeScale.Body,
    bodySmall = BooqiTypeScale.Caption,
    labelLarge = BooqiTypeScale.Label,
    labelMedium = BooqiTypeScale.Label,
    labelSmall = BooqiTypeScale.Caption,
)
