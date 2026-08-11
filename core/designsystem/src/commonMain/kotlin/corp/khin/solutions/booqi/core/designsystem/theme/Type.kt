package corp.khin.solutions.booqi.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp

// Placeholder type scale — swap for the real scale from the Figma foundations page. Kept as a
// single Typography instance (Material 3's default role set: display/headline/title/body/label)
// so every screen pulls from MaterialTheme.typography instead of hardcoding text styles.
@Composable
fun booqiTypography(): Typography {
    val base = TextStyle(fontSize = 16.sp)
    return Typography(
        bodyLarge = base.copy(fontSize = 16.sp),
        bodyMedium = base.copy(fontSize = 14.sp),
        bodySmall = base.copy(fontSize = 12.sp),
        titleLarge = base.copy(fontSize = 22.sp),
        titleMedium = base.copy(fontSize = 16.sp),
        titleSmall = base.copy(fontSize = 14.sp),
        labelLarge = base.copy(fontSize = 14.sp),
        labelMedium = base.copy(fontSize = 12.sp),
        labelSmall = base.copy(fontSize = 11.sp),
    )
}
