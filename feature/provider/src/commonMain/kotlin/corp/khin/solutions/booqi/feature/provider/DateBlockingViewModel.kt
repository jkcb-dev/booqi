package corp.khin.solutions.booqi.feature.provider

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import corp.khin.solutions.booqi.core.common.DomainError
import corp.khin.solutions.booqi.core.common.DomainResult
import corp.khin.solutions.booqi.domain.model.Availability
import corp.khin.solutions.booqi.domain.model.BlockedPeriod
import corp.khin.solutions.booqi.domain.model.TimeRange
import corp.khin.solutions.booqi.domain.usecase.BloquearFechaHoraUseCase
import corp.khin.solutions.booqi.domain.usecase.DesbloquearFechaHoraUseCase
import corp.khin.solutions.booqi.domain.usecase.ObtenerHorarioUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn

/**
 * P7 — the date-blocking calendar, the other half of `Destination.ScheduleManagement` (see
 * [WeeklyScheduleViewModel]). Talks to the domain only through [ObtenerHorarioUseCase]/
 * [BloquearFechaHoraUseCase]/[DesbloquearFechaHoraUseCase]; every one of them answers with the
 * Provider's whole [Availability], whose `blockedPeriods` replace the state's list as-is.
 *
 * "Today" is read from the injected [clock] (default `Clock.System`) in [timeZone], on construction
 * and on every `Start`, so the highlighted day is right after the app was left open past midnight.
 *
 * **No init-time load** — same reason as [WeeklyScheduleViewModel]: the screen sends
 * [DateBlockingAction.Start] on every entry.
 */
class DateBlockingViewModel(
    private val obtenerHorario: ObtenerHorarioUseCase,
    private val bloquearFechaHora: BloquearFechaHoraUseCase,
    private val desbloquearFechaHora: DesbloquearFechaHoraUseCase,
    private val providerId: String = TEMPORARY_PROVIDER_ID,
    private val clock: Clock = Clock.System,
    private val timeZone: TimeZone = TimeZone.currentSystemDefault(),
) : ViewModel() {

    private var loadJob: Job? = null

    private val _state = MutableStateFlow(initialState())
    val state: StateFlow<DateBlockingUiState> = _state.asStateFlow()

    private val _events = Channel<DateBlockingEvent>()
    val events = _events.receiveAsFlow()

    fun onAction(action: DateBlockingAction) {
        when (action) {
            DateBlockingAction.Start -> start()
            DateBlockingAction.PreviousMonth -> shiftMonth(-1)
            DateBlockingAction.NextMonth -> shiftMonth(1)
            is DateBlockingAction.DateTapped -> toggleWholeDay(action.date)
            is DateBlockingAction.Unblock -> submit { desbloquearFechaHora(providerId, action.period) }
            DateBlockingAction.ShowRangeForm ->
                _state.update { it.copy(rangeForm = BlockRangeFormState(date = it.today)) }
            DateBlockingAction.DismissRangeForm -> _state.update { it.copy(rangeForm = null) }
            is DateBlockingAction.RangeDateChanged -> editForm { it.copy(date = action.date) }
            is DateBlockingAction.RangeStartChanged -> editForm { it.copy(startInput = action.value) }
            is DateBlockingAction.RangeEndChanged -> editForm { it.copy(endInput = action.value) }
            DateBlockingAction.ConfirmRange -> confirmRange()
        }
    }

    private fun initialState(): DateBlockingUiState {
        val today = clock.todayIn(timeZone)
        return DateBlockingUiState(today = today, visibleMonth = firstOfMonth(today))
    }

    /** Escenario: "El Proveedor consulta su horario" — blocked dates in chronological order (the
     * repository's contract), and a Provider without any gets an empty calendar, not an error. */
    private fun start() {
        loadJob?.cancel()
        _state.value = initialState()
        loadJob = viewModelScope.launch {
            when (val result = obtenerHorario(providerId)) {
                is DomainResult.Success -> _state.update {
                    it.copy(isLoading = false, blockedPeriods = result.value.blockedPeriods)
                }
                is DomainResult.Failure -> {
                    _state.update { it.copy(isLoading = false, loadError = result.error) }
                    _events.send(DateBlockingEvent.ShowError(result.error.describe(NOT_FOUND_MESSAGE)))
                }
            }
        }
    }

    private fun shiftMonth(months: Int) {
        _state.update { it.copy(visibleMonth = it.visibleMonth.shiftedByMonths(months)) }
    }

    private fun editForm(edit: (BlockRangeFormState) -> BlockRangeFormState) {
        _state.update { state ->
            state.copy(rangeForm = state.rangeForm?.let { edit(it).copy(error = null) })
        }
    }

    /** Escenarios: "El Proveedor bloquea un día específico" / "...desbloquea una fecha" — a tap
     * on a free (or only partially blocked) date blocks it whole, a tap on a fully blocked one
     * frees that whole-day block. */
    private fun toggleWholeDay(date: LocalDate) {
        val current = _state.value
        if (current.isLoading || current.loadError != null) return
        val period = BlockedPeriod(date)
        submit {
            if (period in current.blockedPeriods) {
                desbloquearFechaHora(providerId, period)
            } else {
                bloquearFechaHora(providerId, period)
            }
        }
    }

    /** Escenarios: "El Proveedor bloquea solo un rango de horas de un día" / "...un rango de horas
     * inválido" — unparseable text is a form error; an inverted range is the use case's call, and
     * its `InvalidInput` lands on the same form. */
    private fun confirmRange() {
        val form = _state.value.rangeForm ?: return
        val start = parseTimeInput(form.startInput)
        val end = parseTimeInput(form.endInput)
        if (start == null || end == null) {
            editFormError(INVALID_TIME_MESSAGE)
            return
        }
        submit(formRule = FormRule.ClosesOnSuccess) {
            bloquearFechaHora(providerId, BlockedPeriod(form.date, TimeRange(start, end)))
        }
    }

    private fun editFormError(message: String) {
        _state.update { state -> state.copy(rangeForm = state.rangeForm?.copy(error = message)) }
    }

    /** What a call means for the "bloquear horas" form. */
    private enum class FormRule { Ignores, ClosesOnSuccess }

    /** Runs one block/unblock call: ignored while another is in flight. On success the returned
     * periods replace the list (and the range form closes for [FormRule.ClosesOnSuccess]). An
     * `InvalidInput` is a range-form error for the form's call; every other failure goes to
     * [DateBlockingUiState.actionError]. */
    private fun submit(
        formRule: FormRule = FormRule.Ignores,
        call: suspend () -> DomainResult<Availability>,
    ) {
        if (_state.value.isUpdating) return
        // Flagged synchronously, before the coroutine runs, so a second tap cannot slip past the
        // isUpdating guard above.
        _state.update { it.copy(isUpdating = true, actionError = null) }
        viewModelScope.launch {
            when (val result = call()) {
                is DomainResult.Success -> _state.update {
                    it.copy(
                        isUpdating = false,
                        blockedPeriods = result.value.blockedPeriods,
                        rangeForm = if (formRule == FormRule.ClosesOnSuccess) null else it.rangeForm,
                    )
                }
                is DomainResult.Failure -> onFailed(result.error, formRule)
            }
        }
    }

    private suspend fun onFailed(error: DomainError, formRule: FormRule) {
        val message = error.describe(NOT_FOUND_MESSAGE)
        _state.update { it.copy(isUpdating = false) }
        if (error is DomainError.InvalidInput && formRule == FormRule.ClosesOnSuccess) {
            editFormError(message)
        } else {
            _state.update { it.copy(actionError = message) }
            _events.send(DateBlockingEvent.ShowError(message))
        }
    }

    private companion object {
        const val NOT_FOUND_MESSAGE = "No se encontró el horario"
    }
}
