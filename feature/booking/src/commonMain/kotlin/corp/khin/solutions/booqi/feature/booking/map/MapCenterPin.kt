package corp.khin.solutions.booqi.feature.booking.map

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import corp.khin.solutions.booqi.core.designsystem.theme.BooqiSpacing

/** Fixed center pin overlaid on the platform map (both actuals). */
@Composable
internal fun MapCenterPin(modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(BooqiSpacing.lg)
                .background(MaterialTheme.colorScheme.primary, CircleShape),
        )
        Box(
            modifier = Modifier
                .size(BooqiSpacing.xxs)
                .background(MaterialTheme.colorScheme.primary, CircleShape),
        )
    }
}
