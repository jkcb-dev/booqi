package corp.khin.solutions.booqi.feature.booking

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextDecoration
import corp.khin.solutions.booqi.core.designsystem.theme.BooqiSpacing
import corp.khin.solutions.booqi.domain.model.TimeSlot

/**
 * `TimeSlotGrid` organism (docs/design/DESIGN_SYSTEM.md — grid of time-slot cells, the unavailable
 * ones struck through; owned by `feature:booking`, Customer slot selection only). Each cell is the
 * "Time slot cell" molecule: a start time that is tappable when [SlotCell.isAvailable] and
 * struck-through and disabled otherwise. [selected] is highlighted. The grid wraps; it sits inside
 * a parent that already scrolls.
 */
@Composable
internal fun TimeSlotGrid(
    cells: List<SlotCell>,
    selected: TimeSlot?,
    onSelect: (TimeSlot) -> Unit,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(BooqiSpacing.xs),
        verticalArrangement = Arrangement.spacedBy(BooqiSpacing.xs),
    ) {
        cells.forEach { cell ->
            TimeSlotCell(cell = cell, isSelected = cell.slot == selected, onClick = { onSelect(cell.slot) })
        }
    }
}

@Composable
private fun TimeSlotCell(cell: SlotCell, isSelected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = isSelected,
        onClick = onClick,
        enabled = cell.isAvailable,
        label = {
            val decoration = if (cell.isAvailable) TextDecoration.None else TextDecoration.LineThrough
            Text(text = formatTime(cell.slot.start), style = TextStyle(textDecoration = decoration))
        },
    )
}
