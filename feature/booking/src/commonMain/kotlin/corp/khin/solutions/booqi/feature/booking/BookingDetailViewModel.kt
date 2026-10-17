package corp.khin.solutions.booqi.feature.booking

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import corp.khin.solutions.booqi.core.common.DomainError
import corp.khin.solutions.booqi.core.common.DomainResult
import corp.khin.solutions.booqi.domain.model.Booking
import corp.khin.solutions.booqi.domain.model.BookingStatus
import corp.khin.solutions.booqi.domain.model.CustomerReasonCode
import corp.khin.solutions.booqi.domain.usecase.CancelarReservaClienteUseCase
import corp.khin.solutions.booqi.domain.usecase.ObtenerReservaUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlin.time.Clock

/**
 * C8-C10b (+ completed / cancelled) — one of the Customer's Bookings and what they can do with it
 * (docs/domain/customer-flow.md § Grupo 3). Talks to the domain through [ObtenerReservaUseCase],
 * [CancelarReservaClienteUseCase] and [BookingLookups] (service title, provider name).
 *
 * **Re-entry.** Not destination-scoped (docs/DEVELOPMENT.md): the screen sends
 * [BookingDetailAction.Start] on every entry, which wipes the previous Booking and reloads, so a
 * status that changed elsewhere (the Provider answered) is always re-read.
 *
 * **Cancel.** "Cancelar cita" opens the reason picker only while `now <= start - 3h` (inclusive,
 * on the injected [clock] in [timeZone] — [BookingDetailUiState.canCancel]); inside the window the
 * action is refused with the domain's wording. The domain re-checks, so a stale screen cannot slip
 * a late cancellation through: its `InvalidInput` message is shown inline and the Booking re-read.
 */
class BookingDetailViewModel(
    private val obtenerReserva: ObtenerReservaUseCase,
    private val cancelarReserva: CancelarReservaClienteUseCase,
    private val lookups: BookingLookups,
    private val clock: Clock = Clock.System,
    private val timeZone: TimeZone = TimeZone.currentSystemDefault(),
) : ViewModel() {

    private val _state = MutableStateFlow(BookingDetailUiState(now = clock.now(), timeZone = timeZone))
    val state: StateFlow<BookingDetailUiState> = _state.asStateFlow()

    private val _events = Channel<BookingDetailEvent>()
    val events = _events.receiveAsFlow()

    private var loadJob: Job? = null

    fun onAction(action: BookingDetailAction) {
        when (action) {
            is BookingDetailAction.Start -> start(action.bookingId)
            BookingDetailAction.Retry -> _state.value.bookingId?.let(::start)
            BookingDetailAction.Tick -> _state.update { it.copy(now = clock.now()) }
            BookingDetailAction.OpenCancelForm -> openCancelForm()
            BookingDetailAction.CloseCancelForm ->
                _state.update { it.copy(cancelForm = null, actionError = null) }
            is BookingDetailAction.ReasonSelected ->
                _state.updateForm { it.copy(code = action.code, error = null) }
            is BookingDetailAction.ReasonNoteChanged -> _state.updateForm { it.copy(note = action.value) }
            BookingDetailAction.ConfirmCancel -> confirmCancel()
            BookingDetailAction.DismissNotice -> _state.update { it.copy(notice = null) }
            BookingDetailAction.RateClicked -> rate()
            BookingDetailAction.RebookClicked -> rebook()
            BookingDetailAction.GoHomeClicked -> emit(BookingDetailEvent.GoHome)
            BookingDetailAction.BackClicked -> back()
        }
    }

    private fun emit(event: BookingDetailEvent) {
        viewModelScope.launch { _events.send(event) }
    }

    private fun rate() {
        val state = _state.value
        val bookingId = state.bookingId
        if (state.canRate && bookingId != null) emit(BookingDetailEvent.Rate(bookingId))
    }

    private fun rebook() {
        _state.value.booking?.let { emit(BookingDetailEvent.Rebook(it.serviceId)) }
    }

    private fun back() {
        if (_state.value.cancelForm != null && !_state.value.isCancelling) {
            _state.update { it.copy(cancelForm = null, actionError = null) }
        } else {
            emit(BookingDetailEvent.Finished)
        }
    }

    private fun start(bookingId: String) {
        loadJob?.cancel()
        _state.value = BookingDetailUiState(now = clock.now(), timeZone = timeZone, bookingId = bookingId)
        loadJob = viewModelScope.launch {
            when (val result = obtenerReserva(bookingId)) {
                is DomainResult.Failure -> _state.update { it.copy(isLoading = false, error = result.error) }
                is DomainResult.Success -> {
                    val booking = result.value
                    _state.update { it.copy(isLoading = false, booking = booking, now = clock.now()) }
                    val title = lookups.serviceTitle(booking.serviceId)
                    val provider = lookups.providerName(booking.providerId)
                    _state.update { it.copy(serviceTitle = title, providerName = provider) }
                }
            }
        }
    }

    private fun openCancelForm() {
        val current = _state.value
        val booking = current.booking?.takeIf { it.status == BookingStatus.CONFIRMED } ?: return
        val now = clock.now()
        _state.update { it.copy(now = now) }
        if (booking.canBeCancelledByCustomer(now, timeZone)) {
            _state.update { it.copy(cancelForm = CancelFormState(), actionError = null) }
        } else {
            _state.update { it.copy(actionError = Booking.TOO_LATE_TO_CANCEL_MESSAGE) }
        }
    }

    /** Escenarios: "El Cliente cancela una reserva confirmada a tiempo" / "...fuera del plazo
     * permitido". A reason is required; the free text only travels with "Otro". */
    private fun confirmCancel() {
        val current = _state.value
        val form = current.cancelForm
        val booking = current.booking
        val code = form?.code
        when {
            form == null || booking == null || current.isCancelling -> Unit
            code == null -> _state.updateForm { it.copy(error = REASON_REQUIRED_MESSAGE) }
            else -> cancel(booking.id, code, form.note.takeIf { code == CustomerReasonCode.OTHER })
        }
    }

    private fun cancel(bookingId: String, code: CustomerReasonCode, note: String?) {
        _state.update { it.copy(isCancelling = true, actionError = null, now = clock.now()) }
        viewModelScope.launch {
            when (val result = cancelarReserva(bookingId, code, note)) {
                is DomainResult.Success -> _state.update {
                    it.copy(
                        booking = result.value,
                        cancelForm = null,
                        isCancelling = false,
                        notice = NOTICE_CANCELLED,
                    )
                }
                is DomainResult.Failure -> failCancel(bookingId, result.error)
            }
        }
    }

    private suspend fun failCancel(bookingId: String, error: DomainError) {
        _state.update {
            it.copy(isCancelling = false, cancelForm = null, actionError = error.describe(NOT_FOUND_MESSAGE))
        }
        if (error is DomainError.InvalidInput) {
            (obtenerReserva(bookingId) as? DomainResult.Success)?.let { fresh ->
                _state.update { it.copy(booking = fresh.value, now = clock.now()) }
            }
        }
    }

    private companion object {
        const val NOT_FOUND_MESSAGE = "No se encontró la reserva"
        const val REASON_REQUIRED_MESSAGE = "Elige un motivo para continuar"
        const val NOTICE_CANCELLED = "Cita cancelada"
    }
}

private fun MutableStateFlow<BookingDetailUiState>.updateForm(change: (CancelFormState) -> CancelFormState) {
    update { state -> state.cancelForm?.let { state.copy(cancelForm = change(it)) } ?: state }
}
