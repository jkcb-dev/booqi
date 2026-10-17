package corp.khin.solutions.booqi.feature.booking

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import corp.khin.solutions.booqi.core.common.DomainResult
import corp.khin.solutions.booqi.domain.model.Rating
import corp.khin.solutions.booqi.domain.usecase.CalificarCitaUseCase
import corp.khin.solutions.booqi.domain.usecase.ObtenerReservaUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * C12 — rating a completed appointment: 1 to 5 stars and an optional comment (docs/domain/
 * provider-flow.md § Grupo 4, "El Cliente califica una cita completada" / "...no completada").
 * Talks to the domain through [ObtenerReservaUseCase] (to know the Booking's status and whether it
 * is already rated), [CalificarCitaUseCase] and [BookingLookups] (service title). Only a COMPLETED,
 * not yet rated Booking can be submitted ([RateBookingUiState.canSubmit]); the domain enforces the
 * same rules, so a stale form's rejection is shown inline.
 *
 * **Re-entry.** [RateBookingAction.Start] on every entry resets the form and reloads (the instance
 * is not destination-scoped, docs/DEVELOPMENT.md): rating one booking and then another never
 * shows the previous stars or comment.
 */
class RateBookingViewModel(
    private val obtenerReserva: ObtenerReservaUseCase,
    private val calificarCita: CalificarCitaUseCase,
    private val lookups: BookingLookups,
) : ViewModel() {

    private val _state = MutableStateFlow(RateBookingUiState())
    val state: StateFlow<RateBookingUiState> = _state.asStateFlow()

    private val _events = Channel<RateBookingEvent>()
    val events = _events.receiveAsFlow()

    private var loadJob: Job? = null

    fun onAction(action: RateBookingAction) {
        when (action) {
            is RateBookingAction.Start -> load(action.bookingId)
            RateBookingAction.Retry -> _state.value.bookingId?.let(::load)
            is RateBookingAction.StarsSelected -> if (action.stars in Rating.MIN_STARS..Rating.MAX_STARS) {
                _state.update { it.copy(stars = action.stars, formError = null) }
            }
            is RateBookingAction.CommentChanged -> _state.update { it.copy(comment = action.value) }
            RateBookingAction.Submit -> submit()
            RateBookingAction.BackClicked -> viewModelScope.launch { _events.send(RateBookingEvent.Finished) }
        }
    }

    private fun load(bookingId: String) {
        loadJob?.cancel()
        _state.value = RateBookingUiState(bookingId = bookingId)
        loadJob = viewModelScope.launch {
            when (val result = obtenerReserva(bookingId)) {
                is DomainResult.Failure -> _state.update { it.copy(isLoading = false, error = result.error) }
                is DomainResult.Success -> {
                    _state.update { it.copy(isLoading = false, booking = result.value) }
                    val title = lookups.serviceTitle(result.value.serviceId)
                    _state.update { it.copy(serviceTitle = title) }
                }
            }
        }
    }

    private fun submit() {
        val current = _state.value
        val bookingId = current.bookingId ?: return
        if (!current.canSubmit) return
        _state.update { it.copy(isSubmitting = true, formError = null) }
        viewModelScope.launch {
            when (val result = calificarCita(bookingId, current.stars, current.comment)) {
                is DomainResult.Success -> {
                    _state.update { it.copy(isSubmitting = false, booking = result.value) }
                    _events.send(RateBookingEvent.Finished)
                }
                is DomainResult.Failure -> _state.update {
                    it.copy(isSubmitting = false, formError = result.error.describe(NOT_FOUND_MESSAGE))
                }
            }
        }
    }

    private companion object {
        const val NOT_FOUND_MESSAGE = "No se encontró la reserva"
    }
}
