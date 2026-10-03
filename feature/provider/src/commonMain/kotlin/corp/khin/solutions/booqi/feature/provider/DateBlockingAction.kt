package corp.khin.solutions.booqi.feature.provider

import corp.khin.solutions.booqi.domain.model.BlockedPeriod
import kotlinx.datetime.LocalDate

/** User intents on the date-blocking calendar (P7). */
sealed interface DateBlockingAction {
    /**
     * Sent by [ScheduleManagementScreen] every time it enters composition, and by the retry
     * button: back to the current month with the form closed, and (re)loads the blocked periods.
     * Needed because ViewModels are not destination-scoped (see docs/DEVELOPMENT.md).
     */
    data object Start : DateBlockingAction

    data object PreviousMonth : DateBlockingAction
    data object NextMonth : DateBlockingAction

    /** Tap on a calendar date: blocks the whole day, or unblocks it if it is blocked whole-day. */
    data class DateTapped(val date: LocalDate) : DateBlockingAction

    /** Removes exactly [period] (a whole day or one time range) — the list's "Desbloquear". */
    data class Unblock(val period: BlockedPeriod) : DateBlockingAction

    data object ShowRangeForm : DateBlockingAction
    data object DismissRangeForm : DateBlockingAction
    data class RangeDateChanged(val date: LocalDate) : DateBlockingAction
    data class RangeStartChanged(val value: String) : DateBlockingAction
    data class RangeEndChanged(val value: String) : DateBlockingAction
    data object ConfirmRange : DateBlockingAction
}
