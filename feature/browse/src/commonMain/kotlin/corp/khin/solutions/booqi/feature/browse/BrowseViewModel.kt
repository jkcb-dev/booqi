package corp.khin.solutions.booqi.feature.browse

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import corp.khin.solutions.booqi.core.common.DomainResult
import corp.khin.solutions.booqi.domain.model.GeoPoint
import corp.khin.solutions.booqi.domain.model.ServiceSearchCriteria
import corp.khin.solutions.booqi.domain.usecase.BuscarServiciosUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * C1 + C2 — the Customer's search. Talks to the domain only through [BuscarServiciosUseCase] (one
 * query: text, category chip and distance combine — docs/domain/customer-flow.md § Grupo 1). Single
 * [onAction] entry point keeps this reducer-shaped and easy to test without touching Compose.
 *
 * **No init-time load.** ViewModels are not destination-scoped by our simple navigator (see
 * docs/DEVELOPMENT.md), so this instance is reused when the screen comes back from a detail.
 * [BrowseScreen] sends [BrowseAction.Refresh] on every entry, which re-runs the current search.
 *
 * **Recent searches live here, in memory, for the app session only**: there is no local storage
 * yet (#9), so they are gone when the process dies. Only submitted *text* counts (not chip or
 * distance taps), blank text is ignored, repeats move to the top (ignoring case) and only the
 * latest [RECENT_SEARCHES_LIMIT] are kept.
 *
 * [customerLocation] is the TEMPORARY fixed point of [TEMPORARY_CUSTOMER_LOCATION] until platform
 * location exists (#24). It is always sent, so every result carries its distance, and the distance
 * chip only adds the radius.
 */
class BrowseViewModel(
    private val buscarServicios: BuscarServiciosUseCase,
    private val customerLocation: GeoPoint = TEMPORARY_CUSTOMER_LOCATION,
) : ViewModel() {

    private val _state = MutableStateFlow(BrowseUiState())
    val state: StateFlow<BrowseUiState> = _state.asStateFlow()

    private val _events = Channel<BrowseEvent>()
    val events = _events.receiveAsFlow()

    private var searchJob: Job? = null

    fun onAction(action: BrowseAction) {
        when (action) {
            BrowseAction.Refresh -> if (_state.value.hasSearched) search()
            is BrowseAction.QueryChanged -> _state.update { it.copy(query = action.text) }
            BrowseAction.SearchSubmitted -> {
                rememberRecent(_state.value.query)
                search()
            }
            is BrowseAction.CategorySelected -> {
                _state.update { it.copy(category = action.category) }
                search()
            }
            is BrowseAction.DistanceSelected -> {
                _state.update { it.copy(radiusKm = action.km) }
                search()
            }
            is BrowseAction.RecentSelected -> {
                _state.update { it.copy(query = action.text) }
                rememberRecent(action.text)
                search()
            }
            BrowseAction.ClearSearch -> clear()
            is BrowseAction.ServiceSelected -> viewModelScope.launch {
                _events.send(BrowseEvent.NavigateToServiceDetail(action.serviceId))
            }
        }
    }

    /**
     * Escenarios: "El Cliente busca un Servicio por texto" / "...por categoría (chip)" / "...filtra
     * por distancia" and "Sin coincidencias no es un error". A newer search cancels the one in
     * flight, so a slow earlier answer can't overwrite a later one.
     */
    private fun search() {
        searchJob?.cancel()
        val current = _state.value
        _state.update { it.copy(hasSearched = true, isLoading = true, error = null) }
        val criteria = ServiceSearchCriteria(
            text = current.query.trim().ifEmpty { null },
            category = current.category,
            location = customerLocation,
            radiusKm = current.radiusKm?.toDouble(),
        )
        searchJob = viewModelScope.launch {
            when (val result = buscarServicios(criteria)) {
                is DomainResult.Success -> _state.update {
                    it.copy(isLoading = false, results = result.value)
                }
                is DomainResult.Failure -> _state.update {
                    it.copy(isLoading = false, results = emptyList(), error = result.error)
                }
            }
        }
    }

    private fun clear() {
        searchJob?.cancel()
        _state.update { BrowseUiState(recentSearches = it.recentSearches) }
    }

    /** Escenario: "Recientes" — newest first, no duplicates (ignoring case), at most the limit. */
    private fun rememberRecent(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        _state.update { current ->
            val others = current.recentSearches.filterNot { it.equals(trimmed, ignoreCase = true) }
            current.copy(recentSearches = (listOf(trimmed) + others).take(RECENT_SEARCHES_LIMIT))
        }
    }
}
