package corp.khin.solutions.booqi.feature.provider

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import corp.khin.solutions.booqi.core.common.DomainError
import corp.khin.solutions.booqi.core.common.DomainResult
import corp.khin.solutions.booqi.domain.model.Booking
import corp.khin.solutions.booqi.domain.model.BookingStatus
import corp.khin.solutions.booqi.domain.model.ProviderReasonCode
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone

/**
 * P8–P10 — the Provider's booking inbox, behind `Destination.BookingRequestInbox`: the pending
 * requests ("Solicitudes"), the confirmed appointments ("Confirmadas") and the in-screen detail of
 * either. Talks to the domain only through the use cases grouped in [BookingInboxQueries] and
 * [BookingInboxCommands]. Single [onAction] entry point, same shape as [ServiceListViewModel].
 *
 * **No init-time load.** The instance outlives a visit (not destination-scoped, see
 * docs/DEVELOPMENT.md): the screen sends [BookingInboxAction.Start] on every entry, which resets
 * the tab and any open detail and reloads, so a request answered elsewhere never lingers.
 *
 * **Completar only after the appointment ends** (docs/domain/provider-flow.md says the domain
 * leaves this to the UI): [BookingInboxAction.Complete] is refused until the booking's end time
 * has passed on the injected [clock] in [timeZone], and the screen shows the button disabled
 * until then ([BookingInboxAction.Tick] keeps `now` fresh).
 *
 * [providerId] defaults to the TEMPORARY placeholder (see [TEMPORARY_PROVIDER_ID]).
 */
class BookingInboxViewModel(
    private val queries: BookingInboxQueries,
    private val commands: BookingInboxCommands,
    private val clock: Clock = SystemClock,
    private val timeZone: TimeZone = TimeZone.currentSystemDefault(),
    private val providerId: String = TEMPORARY_PROVIDER_ID,
) : ViewModel() {

    private var loadJob: Job? = null

    private val _state = MutableStateFlow(BookingInboxUiState(now = clock.now(), timeZone = timeZone))
    val state: StateFlow<BookingInboxUiState> = _state.asStateFlow()

    private val _events = Channel<BookingInboxEvent>()
    val events = _events.receiveAsFlow()

    fun onAction(action: BookingInboxAction) {
        when (action) {
            BookingInboxAction.Start -> start()
            BookingInboxAction.Refresh -> loadLists()
            BookingInboxAction.Tick -> _state.update { it.copy(now = clock.now()) }
            is BookingInboxAction.SelectTab -> _state.update { it.copy(tab = action.tab, notice = null) }
            is BookingInboxAction.OpenBooking -> openBooking(action.bookingId, action.reasonIntent)
            BookingInboxAction.BackClicked -> back()
            BookingInboxAction.DismissNotice -> _state.update { it.copy(notice = null) }
            is BookingInboxAction.Accept -> runTransition(action.bookingId, NOTICE_ACCEPTED) { commands.accept(it) }
            is BookingInboxAction.OpenReasonForm -> _state.updateDetail {
                it.copy(reasonForm = ReasonFormState(action.intent), actionError = null)
            }
            is BookingInboxAction.ReasonSelected -> _state.updateForm { it.copy(code = action.code, error = null) }
            is BookingInboxAction.ReasonNoteChanged -> _state.updateForm { it.copy(note = action.value) }
            BookingInboxAction.ConfirmReason -> confirmReason()
            BookingInboxAction.Complete -> complete()
        }
    }

    private fun start() {
        _state.value = BookingInboxUiState(now = clock.now(), timeZone = timeZone)
        loadLists()
    }

    /** Escenario: "El Proveedor consulta su bandeja de solicitudes pendientes" (only the answerable
     * ones, oldest first; none is an empty list, not an error) plus the confirmed agenda. A failed
     * load leaves what is on screen and sets [BookingInboxUiState.loadError]. */
    private fun loadLists() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _state.update { it.copy(isLoading = true, loadError = null, now = clock.now()) }
            val pending = queries.pendingRequests(providerId)
            val confirmed = queries.agenda(providerId, setOf(BookingStatus.CONFIRMED))
            val services = queries.services(providerId)
            val failure = (pending as? DomainResult.Failure) ?: (confirmed as? DomainResult.Failure)
            _state.update { current ->
                current.copy(
                    isLoading = false,
                    loadError = failure?.error,
                    requests = (pending as? DomainResult.Success)?.value ?: current.requests,
                    confirmed = (confirmed as? DomainResult.Success)?.value ?: current.confirmed,
                    // Titles are a nicety: if the services can't be read the rows say "Servicio".
                    serviceTitles = (services as? DomainResult.Success)
                        ?.value?.associate { it.id to it.title } ?: current.serviceTitles,
                )
            }
        }
    }

    /** Escenario: "El Proveedor consulta el detalle ... de una reserva". The tapped row shows at
     * once; the fresh copy replaces it, and a booking that no longer exists is reported inline. */
    private fun openBooking(bookingId: String, reasonIntent: ReasonIntent?) {
        val tapped = (_state.value.requests + _state.value.confirmed).firstOrNull { it.id == bookingId } ?: return
        val form = reasonIntent?.let { ReasonFormState(it) }
        _state.update { it.copy(detail = BookingDetailUiState(tapped, reasonForm = form), notice = null) }
        viewModelScope.launch { refreshDetail(bookingId) }
    }

    private suspend fun refreshDetail(bookingId: String) {
        when (val result = queries.booking(bookingId)) {
            is DomainResult.Success -> _state.updateDetail { it.copy(booking = result.value) }
            is DomainResult.Failure -> _state.updateDetail {
                it.copy(actionError = result.error.describe(NOT_FOUND_MESSAGE))
            }
        }
    }

    private fun back() {
        val detail = _state.value.detail
        when {
            detail?.reasonForm != null -> _state.updateDetail { it.copy(reasonForm = null, actionError = null) }
            detail != null -> _state.update { it.copy(detail = null) }
            else -> viewModelScope.launch { _events.send(BookingInboxEvent.Finished) }
        }
    }

    /** Escenarios: "El Proveedor rechaza una solicitud" / "...cancela una cita ya confirmada". A
     * reason is required; the free text is only sent with "Otro" and is optional there. */
    private fun confirmReason() {
        val detail = _state.value.detail ?: return
        val form = detail.reasonForm ?: return
        val code = form.code
        if (code == null) {
            _state.updateForm { it.copy(error = REASON_REQUIRED_MESSAGE) }
        } else {
            val note = form.note.takeIf { code == ProviderReasonCode.OTHER }
            val id = detail.booking.id
            when (form.intent) {
                ReasonIntent.REJECT -> runTransition(id, NOTICE_REJECTED) { commands.reject(it, code, note) }
                ReasonIntent.CANCEL -> runTransition(id, NOTICE_CANCELLED) { commands.cancel(it, code, note) }
            }
        }
    }

    /** Escenario: "El Proveedor marca una cita confirmada como completada" — only once the
     * appointment's end time has passed (re-read from the clock, not the possibly stale `now`). */
    private fun complete() {
        val detail = _state.value.detail ?: return
        val now = clock.now()
        _state.update { it.copy(now = now) }
        if (!detail.booking.hasEnded(now, timeZone)) {
            _state.updateDetail { it.copy(actionError = NOT_ENDED_MESSAGE) }
            return
        }
        runTransition(detail.booking.id, NOTICE_COMPLETED) { commands.complete(it) }
    }

    /** Runs one Provider transition on [bookingId] (from its row or its open detail). Success
     * closes the detail, shows [notice] and reloads the lists. A failure (an invalid transition or
     * a lapsed request is `InvalidInput`) is reported inline — on the detail when it is open, else
     * as an error notice over the list — and the lists reload so they match what is stored. */
    private fun runTransition(
        bookingId: String,
        notice: String,
        command: suspend (bookingId: String) -> DomainResult<Booking>,
    ) {
        if (_state.value.submittingBookingId != null) return
        _state.update { it.copy(submittingBookingId = bookingId) }
        _state.updateDetail { it.copy(actionError = null) }
        viewModelScope.launch {
            when (val result = command(bookingId)) {
                is DomainResult.Success -> {
                    _state.update {
                        it.copy(detail = null, submittingBookingId = null, notice = BookingNotice(notice))
                    }
                    loadLists()
                }
                is DomainResult.Failure -> failTransition(bookingId, result.error)
            }
        }
    }

    private suspend fun failTransition(bookingId: String, error: DomainError) {
        val message = error.describe(NOT_FOUND_MESSAGE)
        _state.update { state ->
            val onDetail = state.detail?.booking?.id == bookingId
            state.copy(
                submittingBookingId = null,
                detail = if (onDetail) state.detail?.copy(reasonForm = null, actionError = message) else state.detail,
                notice = if (onDetail) state.notice else BookingNotice(message, isError = true),
            )
        }
        if (error is DomainError.InvalidInput && _state.value.detail?.booking?.id == bookingId) {
            refreshDetail(bookingId)
        }
        loadLists()
    }

    private companion object {
        const val NOT_FOUND_MESSAGE = "No se encontró la reserva"
        const val REASON_REQUIRED_MESSAGE = "Elige un motivo para continuar"
        const val NOT_ENDED_MESSAGE = "Podrás marcarla como completada cuando termine la cita"
        const val NOTICE_ACCEPTED = "Solicitud aceptada"
        const val NOTICE_REJECTED = "Solicitud rechazada"
        const val NOTICE_COMPLETED = "Cita marcada como completada"
        const val NOTICE_CANCELLED = "Cita cancelada"
    }
}

private fun MutableStateFlow<BookingInboxUiState>.updateDetail(
    change: (BookingDetailUiState) -> BookingDetailUiState,
) {
    update { state -> state.detail?.let { state.copy(detail = change(it)) } ?: state }
}

private fun MutableStateFlow<BookingInboxUiState>.updateForm(change: (ReasonFormState) -> ReasonFormState) {
    updateDetail { detail -> detail.reasonForm?.let { detail.copy(reasonForm = change(it)) } ?: detail }
}
