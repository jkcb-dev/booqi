package corp.khin.solutions.booqi.feature.booking

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import corp.khin.solutions.booqi.core.common.DomainError
import corp.khin.solutions.booqi.core.common.DomainResult
import corp.khin.solutions.booqi.domain.usecase.VerHistorialReservasUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Clock

/**
 * C11 — "Mis reservas": the signed-in Customer's Bookings, separated by status
 * (docs/domain/customer-flow.md § Grupo 3, "El Cliente consulta su historial de reservas"), through
 * [VerHistorialReservasUseCase] (which resolves the user itself and orders deterministically).
 * Tabs Todas / Confirmada / Pendiente / Completada filter the loaded history; no Bookings is an
 * empty list, a visitor (`Unauthorized`) gets a sign-in state — never an error or a crash.
 *
 * **Re-entry.** [MyBookingsAction.Start] on every entry resets the tab and reloads (the instance is
 * not destination-scoped, docs/DEVELOPMENT.md), so a booking just requested or answered appears.
 */
class MyBookingsViewModel(
    private val verHistorial: VerHistorialReservasUseCase,
    private val lookups: BookingLookups,
    private val clock: Clock = Clock.System,
) : ViewModel() {

    private val _state = MutableStateFlow(MyBookingsUiState(now = clock.now()))
    val state: StateFlow<MyBookingsUiState> = _state.asStateFlow()

    private val _events = Channel<MyBookingsEvent>()
    val events = _events.receiveAsFlow()

    private var loadJob: Job? = null

    fun onAction(action: MyBookingsAction) {
        when (action) {
            MyBookingsAction.Start -> {
                _state.value = MyBookingsUiState(now = clock.now())
                load()
            }
            MyBookingsAction.Retry -> load()
            is MyBookingsAction.SelectTab -> _state.update { it.copy(tab = action.tab) }
            is MyBookingsAction.BookingClicked -> emit(MyBookingsEvent.BookingSelected(action.bookingId))
            MyBookingsAction.SignInClicked -> emit(MyBookingsEvent.SignInRequired)
            MyBookingsAction.ExploreClicked -> emit(MyBookingsEvent.Explore)
        }
    }

    private fun emit(event: MyBookingsEvent) {
        viewModelScope.launch { _events.send(event) }
    }

    private fun load() {
        loadJob?.cancel()
        _state.update { it.copy(isLoading = true, error = null, needsSignIn = false, now = clock.now()) }
        loadJob = viewModelScope.launch {
            when (val result = verHistorial()) {
                is DomainResult.Success -> {
                    val history = result.value
                    _state.update { it.copy(isLoading = false, history = history) }
                    val titles = lookups.serviceTitles(history.bookings.map { it.serviceId })
                    _state.update { it.copy(titles = titles) }
                }
                is DomainResult.Failure -> _state.update {
                    it.copy(
                        isLoading = false,
                        needsSignIn = result.error == DomainError.Unauthorized,
                        error = result.error.takeUnless { e -> e == DomainError.Unauthorized },
                    )
                }
            }
        }
    }
}
