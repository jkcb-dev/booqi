package corp.khin.solutions.booqi.feature.provider

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import corp.khin.solutions.booqi.core.common.DomainError
import corp.khin.solutions.booqi.core.common.DomainResult
import corp.khin.solutions.booqi.domain.model.Availability
import corp.khin.solutions.booqi.domain.usecase.DefinirHorarioSemanalUseCase
import corp.khin.solutions.booqi.domain.usecase.ModificarHorarioSemanalUseCase
import corp.khin.solutions.booqi.domain.usecase.ObtenerHorarioUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.DayOfWeek

/**
 * P6 — the weekly schedule editor, one half of `Destination.ScheduleManagement` (the other half
 * is [DateBlockingViewModel]; two ViewModels keep each reducer small and independently testable,
 * [ScheduleManagementScreen] hosts both). Talks to the domain only through
 * [ObtenerHorarioUseCase]/[DefinirHorarioSemanalUseCase]/[ModificarHorarioSemanalUseCase].
 *
 * **Define vs Modify.** `Save` calls Define (the whole week) while the last loaded/saved
 * [Availability] has no weekly hours, and Modify (only the days whose rows changed) once it has.
 * Modify fails with `NotFound` on a Provider without a schedule, so the first save must be a
 * Define; afterwards Modify is the better fit because it leaves untouched days exactly as stored
 * (a second device's edit to another day is not overwritten by a stale full-week replace) and
 * mirrors the Provider's intent ("cambia sus horas de un día"). The deciding fact is the domain's
 * own `isScheduleDefined`, refreshed from every use case response, so after a first save the very
 * next one is already a Modify.
 *
 * **No init-time load.** The instance outlives a visit (not destination-scoped, see
 * docs/DEVELOPMENT.md): the screen sends [WeeklyScheduleAction.Start] on every entry, which
 * reloads and drops unsaved edits.
 *
 * [providerId] defaults to the TEMPORARY placeholder (see [TEMPORARY_PROVIDER_ID]).
 */
class WeeklyScheduleViewModel(
    private val obtenerHorario: ObtenerHorarioUseCase,
    private val definirHorario: DefinirHorarioSemanalUseCase,
    private val modificarHorario: ModificarHorarioSemanalUseCase,
    private val providerId: String = TEMPORARY_PROVIDER_ID,
) : ViewModel() {

    private var loadJob: Job? = null

    private val _state = MutableStateFlow(WeeklyScheduleUiState())
    val state: StateFlow<WeeklyScheduleUiState> = _state.asStateFlow()

    private val _events = Channel<WeeklyScheduleEvent>()
    val events = _events.receiveAsFlow()

    fun onAction(action: WeeklyScheduleAction) {
        when (action) {
            WeeklyScheduleAction.Start -> start()
            is WeeklyScheduleAction.DayToggled -> editRow(action.day) { it.copy(isActive = action.isActive) }
            is WeeklyScheduleAction.StartChanged -> editRow(action.day) { it.copy(startInput = action.value) }
            is WeeklyScheduleAction.EndChanged -> editRow(action.day) { it.copy(endInput = action.value) }
            WeeklyScheduleAction.Save -> save()
        }
    }

    /** Escenario: "El Proveedor consulta su horario" — Monday to Sunday, and a Provider without a
     * schedule gets seven inactive default rows, not an error. */
    private fun start() {
        loadJob?.cancel()
        _state.value = WeeklyScheduleUiState()
        loadJob = viewModelScope.launch {
            when (val result = obtenerHorario(providerId)) {
                is DomainResult.Success -> _state.update { it.loadedFrom(result.value) }
                is DomainResult.Failure -> {
                    _state.update { it.copy(isLoading = false, loadError = result.error) }
                    _events.send(WeeklyScheduleEvent.ShowError(result.error.describe(NOT_FOUND_MESSAGE)))
                }
            }
        }
    }

    private fun WeeklyScheduleUiState.loadedFrom(availability: Availability) = copy(
        isLoading = false,
        rows = availability.toRows(),
        savedHours = availability.weeklyHours,
        loadError = null,
        formError = null,
    )

    /** Editing a row clears its own error (and the form-level one), like the #15 form fields. */
    private fun editRow(day: DayOfWeek, edit: (ScheduleDayRowState) -> ScheduleDayRowState) {
        _state.update { state ->
            // Mid-save the rows are about to be replaced by the saved schedule; an edit now is lost.
            if (state.isSaving) return@update state
            state.copy(
                rows = state.rows.map { if (it.day == day) edit(it).copy(error = null) else it },
                formError = null,
            )
        }
    }

    /** Escenarios: "El Proveedor define su horario semanal" / "...modifica su horario semanal" /
     * "...define o modifica un horario con horas inválidas". */
    private fun save() {
        val current = _state.value
        if (current.isSaving || current.isLoading || current.loadError != null) return
        when (val draft = current.toDraft()) {
            is ScheduleDraft.Invalid -> _state.update { it.copy(rows = draft.rows, formError = null) }
            ScheduleDraft.NothingToSave -> Unit
            is ScheduleDraft.Define -> submit { definirHorario(providerId, draft.week) }
            is ScheduleDraft.Modify -> submit { modificarHorario(providerId, draft.changes) }
        }
    }

    private fun submit(call: suspend () -> DomainResult<Availability>) {
        // Flagged synchronously, before the coroutine runs, so a second tap cannot slip past the
        // isSaving guard in save().
        _state.update { state ->
            state.copy(isSaving = true, formError = null, rows = state.rows.map { it.copy(error = null) })
        }
        viewModelScope.launch {
            when (val result = call()) {
                is DomainResult.Success -> _state.update { it.loadedFrom(result.value).copy(isSaving = false) }
                is DomainResult.Failure -> onSaveFailed(result.error)
            }
        }
    }

    /** The use case's `InvalidInput` is the authority on hours; it does not say which day, so it
     * is shown on the active rows whose range is inverted (and as a form error when none is). */
    private suspend fun onSaveFailed(error: DomainError) {
        val message = error.describe(NOT_FOUND_MESSAGE)
        _state.update { state ->
            val rows = if (error is DomainError.InvalidInput) state.rows.withRangeErrors(message) else state.rows
            state.copy(
                isSaving = false,
                rows = rows,
                formError = message.takeIf { rows.none { it.error != null } },
            )
        }
        if (error !is DomainError.InvalidInput) _events.send(WeeklyScheduleEvent.ShowError(message))
    }

    private companion object {
        const val NOT_FOUND_MESSAGE = "No hay un horario que modificar"
    }
}
