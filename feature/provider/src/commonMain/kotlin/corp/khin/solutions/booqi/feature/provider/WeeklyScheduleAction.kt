package corp.khin.solutions.booqi.feature.provider

import kotlinx.datetime.DayOfWeek

/** User intents on the weekly schedule editor (P6). */
sealed interface WeeklyScheduleAction {
    /**
     * Sent by [ScheduleManagementScreen] every time it enters composition, and by the retry
     * button: (re)loads the schedule and discards unsaved edits. Needed because ViewModels are not
     * destination-scoped (see docs/DEVELOPMENT.md), so a reused instance would otherwise show the
     * previous visit's rows.
     */
    data object Start : WeeklyScheduleAction

    data class DayToggled(val day: DayOfWeek, val isActive: Boolean) : WeeklyScheduleAction
    data class StartChanged(val day: DayOfWeek, val value: String) : WeeklyScheduleAction
    data class EndChanged(val day: DayOfWeek, val value: String) : WeeklyScheduleAction
    data object Save : WeeklyScheduleAction
}
