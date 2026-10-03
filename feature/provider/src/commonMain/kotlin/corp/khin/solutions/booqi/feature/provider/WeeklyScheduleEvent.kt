package corp.khin.solutions.booqi.feature.provider

/**
 * One-shot effects of the weekly editor — never folded into [WeeklyScheduleUiState]. A successful
 * save needs none: "Horario guardado" is derived from the state itself
 * ([WeeklyScheduleUiState.isSaved]).
 */
sealed interface WeeklyScheduleEvent {
    data class ShowError(val message: String) : WeeklyScheduleEvent
}
