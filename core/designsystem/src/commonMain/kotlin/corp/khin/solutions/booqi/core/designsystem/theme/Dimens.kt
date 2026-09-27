package corp.khin.solutions.booqi.core.designsystem.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// Spacing and corner-radius tokens from the Booqi Figma file (Foundations page, Spacing · 8pt
// and Corner Radius sections) — resolves issue #7 alongside Color.kt/Type.kt.
object BooqiSpacing {
    val xxs: Dp = 4.dp
    val xs: Dp = 8.dp
    val sm: Dp = 12.dp
    val md: Dp = 16.dp
    val lg: Dp = 24.dp
    val xl: Dp = 32.dp
    val xxl: Dp = 48.dp
    val xxxl: Dp = 64.dp
}

object BooqiCornerRadius {
    val small: Dp = 10.dp
    val medium: Dp = 16.dp
    val large: Dp = 24.dp
    val pill: Dp = 999.dp
}
