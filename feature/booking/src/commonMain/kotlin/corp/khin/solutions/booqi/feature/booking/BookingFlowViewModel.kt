package corp.khin.solutions.booqi.feature.booking

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import corp.khin.solutions.booqi.core.common.DomainError
import corp.khin.solutions.booqi.core.common.DomainResult
import corp.khin.solutions.booqi.domain.model.DateRange
import corp.khin.solutions.booqi.domain.model.TimeSlot
import corp.khin.solutions.booqi.domain.usecase.SolicitarReservaUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * C5 + C7 — requesting a reservation: pick a TimeSlot of a Service, then confirm and send
 * (docs/domain/customer-flow.md § Grupo 2 and 3). Talks to the domain only through the use cases
 * grouped in [BookingSlotQueries] and [BookingCustomerAccess] plus [SolicitarReservaUseCase].
 *
 * **Re-entry.** The instance outlives a visit (ViewModels are not destination-scoped, see
 * docs/DEVELOPMENT.md): [BookingFlowAction.Start] resets the flow for the Service — except right
 * after the Customer was sent to sign in or to add an address, when coming back to the same Service
 * keeps the chosen slot, note and step and only refreshes the data (the slot may have been taken
 * meanwhile; then the selection is dropped with a message).
 *
 * **Identity.** Sending first runs the access guard: no session -> [AccessIssue.SIGN_IN_REQUIRED],
 * unverified email -> [AccessIssue.EMAIL_NOT_VERIFIED]. Both set the state *and* emit
 * [BookingFlowEvent.SignInRequired]; never a crash. A Domicilio request uses the saved address as
 * the snapshot; without one it is blocked and [BookingFlowEvent.AddAddress] hands off to #23.
 *
 * "Now" comes from the injected [clock] in [timeZone]: slots already in the past today are shown
 * as unavailable (the domain compares no wall-clock slot against now, see
 * [ObtenerTimeSlotsDisponiblesUseCase][corp.khin.solutions.booqi.domain.usecase.ObtenerTimeSlotsDisponiblesUseCase]).
 */
@Suppress("TooManyFunctions")
class BookingFlowViewModel(
    private val slots: BookingSlotQueries,
    private val customer: BookingCustomerAccess,
    private val solicitarReserva: SolicitarReservaUseCase,
    private val clock: Clock = Clock.System,
    private val timeZone: TimeZone = TimeZone.currentSystemDefault(),
) : ViewModel() {

    private val _state = MutableStateFlow(BookingFlowUiState.initial(today()))
    val state: StateFlow<BookingFlowUiState> = _state.asStateFlow()

    private val _events = Channel<BookingFlowEvent>()
    val events = _events.receiveAsFlow()

    private var loadJob: Job? = null
    private var slotsJob: Job? = null
    private var submitJob: Job? = null

    /** Set when the Customer was handed off (sign-in / add address) and is expected back. */
    private var awaitingReturn = false

    fun onAction(action: BookingFlowAction) {
        when (action) {
            is BookingFlowAction.Start -> start(action.serviceId)
            BookingFlowAction.Retry -> retry()
            BookingFlowAction.PreviousDay -> moveDay(-1)
            BookingFlowAction.NextDay -> moveDay(1)
            is BookingFlowAction.SlotSelected -> selectSlot(action.slot)
            BookingFlowAction.ContinueClicked -> continueToConfirm()
            BookingFlowAction.BackClicked -> back()
            is BookingFlowAction.AtHomeChanged -> setAtHome(action.atHome)
            is BookingFlowAction.NoteChanged -> _state.update { it.copy(note = action.value) }
            BookingFlowAction.Submit -> submit()
            BookingFlowAction.AddAddressClicked -> handOff(BookingFlowEvent.AddAddress)
            BookingFlowAction.RetryAddress -> loadAddress()
            BookingFlowAction.SignInClicked -> handOff(BookingFlowEvent.SignInRequired)
        }
    }

    private fun today(): LocalDate = clock.now().toLocalDateTime(timeZone).date

    private fun emit(event: BookingFlowEvent) {
        viewModelScope.launch { _events.send(event) }
    }

    private fun handOff(event: BookingFlowEvent) {
        awaitingReturn = true
        emit(event)
    }

    private fun start(serviceId: String) {
        val resume = awaitingReturn && _state.value.serviceId == serviceId
        awaitingReturn = false
        cancelJobs()
        if (resume) {
            _state.update { it.copy(access = null, submitError = null, isSubmitting = false) }
        } else {
            _state.value = BookingFlowUiState.initial(today(), serviceId)
        }
        load(serviceId, resume)
    }

    private fun retry() {
        _state.value.serviceId?.let { load(it, resume = false) }
    }

    private fun cancelJobs() {
        loadJob?.cancel()
        slotsJob?.cancel()
        submitJob?.cancel()
    }

    /** Loads the Service (C3's `VerDetalleServicio`: a missing/disabled one is `NotFound`), then the
     * slots of the selected day and — on C7 — the saved address. */
    private fun load(serviceId: String, resume: Boolean) {
        loadJob?.cancel()
        _state.update { it.copy(isLoading = true, loadError = null) }
        loadJob = viewModelScope.launch {
            when (val result = slots.detail(serviceId)) {
                is DomainResult.Failure -> _state.update { it.copy(isLoading = false, loadError = result.error) }
                is DomainResult.Success -> {
                    _state.update { it.copy(isLoading = false, detail = result.value) }
                    loadSlots(_state.value.date)
                    if (_state.value.step == BookingStep.CONFIRM) loadAddress()
                    if (resume) dropSelectionIfTaken()
                }
            }
        }
    }

    private fun setAtHome(atHome: Boolean) {
        _state.update { it.copy(atHome = atHome, submitError = null) }
    }

    private fun moveDay(delta: Int) {
        val current = _state.value
        val target = current.date.plus(delta, DateTimeUnit.DAY)
        if (target < current.today || !target.isWithinHorizon(current)) return
        _state.update { it.copy(date = target, slotCells = emptyList(), submitError = null) }
        slotsJob?.cancel()
        slotsJob = viewModelScope.launch { loadSlots(target) }
    }

    private fun LocalDate.isWithinHorizon(state: BookingFlowUiState): Boolean =
        this <= state.today.plus(BookingFlowUiState.BOOKING_HORIZON_DAYS, DateTimeUnit.DAY)

    /**
     * Escenario: "Se seleccionó un TimeSlot disponible". Every slot the schedule produces for the
     * day, flagged with whether it can still be taken: free of other Bookings (by
     * `ObtenerTimeSlotsDisponibles`) and not already in the past.
     */
    private suspend fun loadSlots(date: LocalDate) {
        val service = _state.value.detail?.service ?: return
        _state.update { it.copy(isLoadingSlots = true, slotsError = null) }
        val range = DateRange(date, date)
        val outcome = when (val schedule = slots.schedule(service.providerId)) {
            is DomainResult.Failure -> schedule
            is DomainResult.Success -> when (val all = slots.generate(schedule.value, service.durationMinutes, range)) {
                is DomainResult.Failure -> all
                is DomainResult.Success -> when (
                    val free = slots.available(service.providerId, service.durationMinutes, range)
                ) {
                    is DomainResult.Failure -> free
                    is DomainResult.Success -> {
                        val now = clock.now()
                        DomainResult.Success(
                            all.value.map { slot ->
                                SlotCell(slot, isAvailable = slot in free.value && !slot.isPast(now))
                            },
                        )
                    }
                }
            }
        }
        _state.update { current ->
            if (current.date != date) {
                current
            } else {
                when (outcome) {
                    is DomainResult.Success -> current.copy(isLoadingSlots = false, slotCells = outcome.value)
                    is DomainResult.Failure -> current.copy(isLoadingSlots = false, slotsError = outcome.error)
                }
            }
        }
    }

    private fun TimeSlot.isPast(now: Instant): Boolean =
        LocalDateTime(date, start).toInstant(timeZone) <= now

    private fun selectSlot(slot: TimeSlot) {
        val cell = _state.value.slotCells.firstOrNull { it.slot == slot }
        if (cell?.isAvailable == true) _state.update { it.copy(selectedSlot = slot, submitError = null) }
    }

    private fun dropSelectionIfTaken() {
        val state = _state.value
        val selected = state.selectedSlot ?: return
        if (state.slotCells.none { it.slot == selected && it.isAvailable } && state.date == selected.date) {
            _state.update { it.copy(selectedSlot = null, step = BookingStep.SELECT_SLOT, submitError = SLOT_TAKEN) }
        }
    }

    private fun continueToConfirm() {
        if (_state.value.selectedSlot == null) return
        _state.update { it.copy(step = BookingStep.CONFIRM, submitError = null, access = null) }
        loadAddress()
    }

    private fun back() {
        when (_state.value.step) {
            BookingStep.CONFIRM -> if (!_state.value.isSubmitting) {
                _state.update { it.copy(step = BookingStep.SELECT_SLOT, submitError = null, access = null) }
            }
            BookingStep.SELECT_SLOT -> {
                cancelJobs()
                awaitingReturn = false
                _state.value = BookingFlowUiState.initial(today())
                emit(BookingFlowEvent.Finished)
            }
        }
    }

    /** Escenarios 1 and 2 of Grupo 2: use the saved address, or learn there is none. A visitor has
     * no saved address to read (`Unauthorized`): that is not an error here, sending asks them to
     * sign in. */
    private fun loadAddress() {
        _state.update { it.copy(addressLookup = AddressLookup.PENDING) }
        viewModelScope.launch {
            when (val result = customer.savedAddress()) {
                is DomainResult.Success -> _state.update {
                    it.copy(
                        savedAddress = result.value,
                        addressLookup = if (result.value == null) AddressLookup.MISSING else AddressLookup.FOUND,
                    )
                }
                is DomainResult.Failure -> _state.update {
                    it.copy(
                        savedAddress = null,
                        addressLookup = if (result.error == DomainError.Unauthorized) {
                            AddressLookup.SIGNED_OUT
                        } else {
                            AddressLookup.FAILED
                        },
                    )
                }
            }
        }
    }

    /** Escenarios: "Un visitante intenta reservar sin cuenta" / "con correo sin verificar" (guard
     * first), then "Un Cliente solicita una reserva". */
    private fun submit() {
        val state = _state.value
        val slot = state.selectedSlot
        val serviceId = state.serviceId
        if (!state.canSubmit || slot == null || serviceId == null) return
        _state.update { it.copy(isSubmitting = true, submitError = null, access = null) }
        submitJob?.cancel()
        submitJob = viewModelScope.launch {
            when (val guard = customer.requireVerifiedAccount()) {
                is DomainResult.Failure -> refuseAccess(guard.error)
                is DomainResult.Success -> {
                    val address = state.savedAddress.takeIf { state.wantsDelivery }
                    val result = solicitarReserva(guard.value.id, serviceId, slot, address, state.note)
                    when (result) {
                        is DomainResult.Success -> {
                            awaitingReturn = false
                            _state.update { it.copy(isSubmitting = false) }
                            _events.send(BookingFlowEvent.Done(result.value.id))
                        }
                        is DomainResult.Failure -> failRequest(result.error)
                    }
                }
            }
        }
    }

    private suspend fun refuseAccess(error: DomainError) {
        if (error is DomainError.Unauthorized || error is DomainError.InvalidInput) {
            val issue = if (error is DomainError.Unauthorized) {
                AccessIssue.SIGN_IN_REQUIRED
            } else {
                AccessIssue.EMAIL_NOT_VERIFIED
            }
            awaitingReturn = true
            _state.update { it.copy(isSubmitting = false, access = issue) }
            _events.send(BookingFlowEvent.SignInRequired)
        } else {
            _state.update { it.copy(isSubmitting = false, submitError = error.describe(GENERIC_NOT_FOUND)) }
        }
    }

    /** A rejected request (`InvalidInput`: slot taken, Service disabled, ...) is reported inline and
     * the slots are re-read: a slot that is gone sends the Customer back to pick another. */
    private suspend fun failRequest(error: DomainError) {
        if (error is DomainError.Unauthorized) {
            refuseAccess(error)
            return
        }
        _state.update { it.copy(isSubmitting = false, submitError = error.describe(GENERIC_NOT_FOUND)) }
        if (error is DomainError.InvalidInput) {
            loadSlots(_state.value.date)
            dropSelectionIfTaken()
        }
    }

    private companion object {
        const val SLOT_TAKEN = "El horario seleccionado ya no está disponible. Elige otro."
        const val GENERIC_NOT_FOUND = "No se encontró el servicio"
    }
}
