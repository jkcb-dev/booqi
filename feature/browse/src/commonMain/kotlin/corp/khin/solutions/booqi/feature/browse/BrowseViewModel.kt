package corp.khin.solutions.booqi.feature.browse

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import corp.khin.solutions.booqi.core.common.DomainResult
import corp.khin.solutions.booqi.domain.usecase.GetFeaturedProvidersUseCase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

/**
 * Talks to the domain layer only through [GetFeaturedProvidersUseCase] — never a repository or
 * datasource directly. Single [onAction] entry point keeps this reducer-shaped and easy to test
 * without touching Compose.
 */
class BrowseViewModel(
    private val getFeaturedProviders: GetFeaturedProvidersUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(BrowseUiState())
    val state: StateFlow<BrowseUiState> = _state.asStateFlow()

    private val _events = Channel<BrowseEvent>()
    val events = _events.receiveAsFlow()

    init {
        load()
    }

    fun onAction(action: BrowseAction) {
        when (action) {
            is BrowseAction.Refresh -> load()
            is BrowseAction.SelectProvider -> viewModelScope.launch {
                _events.send(BrowseEvent.NavigateToProviderDetail(action.provider.id))
            }
        }
    }

    private fun load() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            when (val result = getFeaturedProviders()) {
                is DomainResult.Success -> _state.value = _state.value.copy(
                    isLoading = false,
                    providers = result.value,
                )
                is DomainResult.Failure -> {
                    _state.value = _state.value.copy(isLoading = false, error = result.error)
                    _events.send(BrowseEvent.ShowError(result.error.toString()))
                }
            }
        }
    }
}
