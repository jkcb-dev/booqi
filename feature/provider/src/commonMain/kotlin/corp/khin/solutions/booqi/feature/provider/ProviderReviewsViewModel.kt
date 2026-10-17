package corp.khin.solutions.booqi.feature.provider

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import corp.khin.solutions.booqi.core.common.DomainResult
import corp.khin.solutions.booqi.core.common.flatMap
import corp.khin.solutions.booqi.domain.usecase.ObtenerCalificacionesDelProveedorUseCase
import corp.khin.solutions.booqi.domain.usecase.ObtenerUsuarioPublicoUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * P11 — the received ratings (average, count and individual comments) shown on the Provider's
 * completed profile. Talks to the domain only through [ObtenerCalificacionesDelProveedorUseCase].
 *
 * **No init-time load.** The instance outlives a visit (not destination-scoped, see
 * docs/DEVELOPMENT.md), so the section sends [ProviderReviewsAction.Start] every time it enters
 * composition; a rating left since then shows up. There are no one-shot effects, hence no Event.
 *
 * The ratings are the signed-in User's Provider profile's ([CurrentProvider], resolved on every
 * load). Each review carries its author's real name from [ObtenerUsuarioPublicoUseCase] ("Usuario
 * eliminado" for a deleted account).
 */
class ProviderReviewsViewModel(
    private val obtenerCalificaciones: ObtenerCalificacionesDelProveedorUseCase,
    private val obtenerUsuarioPublico: ObtenerUsuarioPublicoUseCase,
    private val currentProvider: CurrentProvider,
) : ViewModel() {

    private var loadJob: Job? = null

    private val _state = MutableStateFlow(ProviderReviewsUiState())
    val state: StateFlow<ProviderReviewsUiState> = _state.asStateFlow()

    fun onAction(action: ProviderReviewsAction) {
        when (action) {
            ProviderReviewsAction.Start -> load()
        }
    }

    /** Escenario: "El Proveedor consulta sus calificaciones" — newest first; none is an empty
     * list, not an error. A failed reload keeps the reviews already shown. */
    private fun load() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            when (val result = currentProvider.profileId().flatMap { obtenerCalificaciones(it) }) {
                is DomainResult.Success -> {
                    val names = obtenerUsuarioPublico.namesOf(result.value.map { it.customerId }.toSet())
                    _state.update { it.copy(isLoading = false, reviews = result.value, authorNames = names) }
                }
                is DomainResult.Failure -> _state.update { it.copy(isLoading = false, error = result.error) }
            }
        }
    }
}
