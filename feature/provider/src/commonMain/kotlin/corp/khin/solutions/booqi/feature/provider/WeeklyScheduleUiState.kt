package corp.khin.solutions.booqi.feature.provider

import corp.khin.solutions.booqi.core.common.DomainError
import corp.khin.solutions.booqi.domain.model.DayHours
import kotlinx.datetime.DayOfWeek

/**
 * One P6 row. [startInput]/[endInput] are the raw text the Provider typed (like the price field
 * of the service form) and are only parsed on save, so a half-typed time is never reformatted
 * under the cursor. [error] is the inline message shown on this row.
 */
data class ScheduleDayRowState(
    val day: DayOfWeek,
    val isActive: Boolean,
    val startInput: String,
    val endInput: String,
    val error: String? = null,
)

/**
 * Immutable state of the weekly schedule editor (docs/design/SCREENS.md P6): always seven [rows],
 * Monday to Sunday — a day the Provider never defined shows as an inactive row with default
 * hours. [savedHours] is the schedule as last loaded/saved from the domain; it is the baseline
 * for "what changed" and, via [isScheduleDefined], decides Define vs Modify on save.
 */
data class WeeklyScheduleUiState(
    /** Starts true so the first frame before the entry `Start` never flashes an empty editor. */
    val isLoading: Boolean = true,
    val rows: List<ScheduleDayRowState> = emptyList(),
    val savedHours: List<DayHours> = emptyList(),
    val isSaving: Boolean = false,
    /** The schedule could not be loaded: the editor body is replaced by a retry. */
    val loadError: DomainError? = null,
    /** A save failure that belongs to no single row (no connection, or an unattributable rule). */
    val formError: String? = null,
) {
    /** True once a weekly schedule exists on the domain side (first save already happened). */
    val isScheduleDefined: Boolean get() = savedHours.isNotEmpty()

    /** True when the rows differ from [savedHours] — there is something to save. */
    val hasChanges: Boolean get() = !isScheduleDefined || rowsDiffer(rows, savedHours)

    /** Shown as "Horario guardado" once saved and untouched since. */
    val isSaved: Boolean get() = isScheduleDefined && !hasChanges
}
