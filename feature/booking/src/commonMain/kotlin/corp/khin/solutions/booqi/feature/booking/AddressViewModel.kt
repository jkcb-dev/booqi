package corp.khin.solutions.booqi.feature.booking

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import corp.khin.solutions.booqi.core.common.DomainError
import corp.khin.solutions.booqi.core.common.DomainResult
import corp.khin.solutions.booqi.domain.model.Address
import corp.khin.solutions.booqi.domain.usecase.ActualizarDireccionUseCase
import corp.khin.solutions.booqi.domain.usecase.AgregarDireccionUseCase
import corp.khin.solutions.booqi.domain.usecase.BuscarDireccionUseCase
import corp.khin.solutions.booqi.domain.usecase.ObtenerMiDireccionUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val SEARCH_DEBOUNCE_MS = 300L

/**
 * C6 — add or edit the Customer's single saved address (docs/domain/customer-flow.md § Grupo 2):
 * search ([BuscarDireccionUseCase]), pick a result or a map pin ([AddressAction.ResultSelected]
 * from either path — the map actual reverse-geocodes, #24), save. The saved address is read on
 * entry ([ObtenerMiDireccionUseCase]); with one the save is `ActualizarDireccion`, without it
 * `AgregarDireccion`.
 *
 * **Re-entry.** [AddressAction.Start] on every entry resets the picker and reloads (the instance is
 * not destination-scoped, docs/DEVELOPMENT.md), so "add -> back -> open again" never shows the
 * previous query or selection, and the "current" address is the one just saved.
 */
class AddressViewModel(
    private val obtenerMiDireccion: ObtenerMiDireccionUseCase,
    private val agregarDireccion: AgregarDireccionUseCase,
    private val actualizarDireccion: ActualizarDireccionUseCase,
    private val buscarDireccion: BuscarDireccionUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(AddressUiState())
    val state: StateFlow<AddressUiState> = _state.asStateFlow()

    private val _events = Channel<AddressEvent>()
    val events = _events.receiveAsFlow()

    private var loadJob: Job? = null
    private var searchJob: Job? = null
    private var saveJob: Job? = null

    fun onAction(action: AddressAction) {
        when (action) {
            AddressAction.Start, AddressAction.Retry -> load()
            is AddressAction.QueryChanged -> changeQuery(action.value)
            is AddressAction.ResultSelected -> _state.update { it.copy(selected = action.address, saveError = null) }
            AddressAction.SaveClicked -> save()
            AddressAction.BackClicked -> viewModelScope.launch { _events.send(AddressEvent.Finished) }
        }
    }

    private fun load() {
        loadJob?.cancel()
        searchJob?.cancel()
        saveJob?.cancel()
        _state.value = AddressUiState()
        loadJob = viewModelScope.launch {
            when (val result = obtenerMiDireccion()) {
                is DomainResult.Success ->
                    _state.update { it.copy(isLoading = false, savedAddress = result.value) }
                is DomainResult.Failure -> _state.update { it.copy(isLoading = false, loadError = result.error) }
            }
        }
    }

    private fun changeQuery(value: String) {
        searchJob?.cancel()
        _state.update { it.copy(query = value, results = emptyList(), searchError = null, isSearching = false) }
        if (value.trim().length < BuscarDireccionUseCase.MIN_QUERY_LENGTH) return
        _state.update { it.copy(isSearching = true) }
        searchJob = viewModelScope.launch {
            delay(SEARCH_DEBOUNCE_MS)
            when (val result = buscarDireccion(value)) {
                is DomainResult.Success -> _state.update { it.copy(isSearching = false, results = result.value) }
                is DomainResult.Failure -> _state.update { it.copy(isSearching = false, searchError = result.error) }
            }
        }
    }

    /** Escenarios "El Cliente agrega su dirección" / "actualiza su dirección guardada" / "intenta
     * guardar una dirección incompleta": a rejected address is a form error and nothing changes. */
    private fun save() {
        val current = _state.value
        val address = current.selected ?: return
        if (!current.canSave) return
        _state.update { it.copy(isSaving = true, saveError = null) }
        saveJob = viewModelScope.launch {
            val result = if (current.savedAddress == null) agregarDireccion(address) else actualizarDireccion(address)
            when (result) {
                is DomainResult.Success -> {
                    _state.update { it.copy(isSaving = false, savedAddress = address) }
                    _events.send(AddressEvent.Finished)
                }
                is DomainResult.Failure ->
                    _state.update { it.copy(isSaving = false, saveError = result.error.describeForAddress()) }
            }
        }
    }

    private fun DomainError.describeForAddress(): String =
        if (this == DomainError.Unauthorized) SIGN_IN_MESSAGE else describe(notFound = SAVE_FAILED_MESSAGE)

    internal companion object {
        const val SIGN_IN_MESSAGE = "Inicia sesión para guardar tu dirección."
        const val SAVE_FAILED_MESSAGE = "No pudimos guardar tu dirección"
    }
}
