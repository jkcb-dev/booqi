package corp.khin.solutions.booqi.feature.provider

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import corp.khin.solutions.booqi.core.designsystem.theme.BooqiCornerRadius
import corp.khin.solutions.booqi.core.designsystem.theme.BooqiSpacing
import kotlinx.datetime.LocalDate

/**
 * One date of the P7 calendar. A fully blocked date is filled with the accent tint
 * (`secondaryContainer`, the same one the paused-profile banner uses); a date with only blocked
 * time ranges gets a dot under its number; today's number sits on a brand-coloured circle, so
 * "today" and "blocked" can show on the same cell. A null [onClick] makes the cell inert (a block/unblock is in
 * flight).
 */
@Composable
internal fun CalendarDayCell(
    date: LocalDate,
    blockState: DateBlockState,
    isToday: Boolean,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val isBlocked = blockState == DateBlockState.FullyBlocked
    Column(
        modifier = modifier
            .aspectRatio(1f)
            .padding(BooqiSpacing.xxs)
            .clip(RoundedCornerShape(BooqiCornerRadius.small))
            .background(if (isBlocked) scheme.secondaryContainer else Color.Transparent)
            .clickable(enabled = onClick != null, onClick = { onClick?.invoke() })
            .semantics { contentDescription = date.accessibilityLabel(blockState, isToday) },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = date.dayOfMonth.toString(),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isToday) FontWeight.Bold else null,
            color = when {
                isToday -> scheme.onPrimary
                isBlocked -> scheme.onSecondaryContainer
                else -> scheme.onSurface
            },
            modifier = if (isToday) {
                Modifier.clip(CircleShape).background(scheme.primary).padding(horizontal = BooqiSpacing.xs)
            } else {
                Modifier
            },
        )
        if (blockState == DateBlockState.PartiallyBlocked) {
            Text("•", style = MaterialTheme.typography.labelSmall, color = scheme.secondary)
        }
    }
}

private fun LocalDate.accessibilityLabel(blockState: DateBlockState, isToday: Boolean): String = buildString {
    append(spanishText())
    if (isToday) append(", hoy")
    when (blockState) {
        DateBlockState.FullyBlocked -> append(", bloqueado todo el día")
        DateBlockState.PartiallyBlocked -> append(", bloqueado en algunas horas")
        DateBlockState.Free -> Unit
    }
}
