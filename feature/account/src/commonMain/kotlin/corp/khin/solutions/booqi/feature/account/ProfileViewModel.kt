package corp.khin.solutions.booqi.feature.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import corp.khin.solutions.booqi.core.common.DomainError
import corp.khin.solutions.booqi.core.common.DomainResult
import corp.khin.solutions.booqi.domain.model.User
import corp.khin.solutions.booqi.domain.usecase.ActualizarPerfilDeUsuarioUseCase
import corp.khin.solutions.booqi.domain.usecase.CerrarSesionUseCase
import corp.khin.solutions.booqi.domain.usecase.ObtenerMiPerfilDeProveedorUseCase
import corp.khin.solutions.booqi.domain.usecase.ObtenerUsuarioActualUseCase
import corp.khin.solutions.booqi.domain.usecase.RequerirCuentaVerificadaUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val PROFILE_UPDATED_MESSAGE = "Perfil actualizado"

/** Everything the profile screen reads from the domain, grouped into one parameter object. */
class ProfileQueries(
    val currentUser: ObtenerUsuarioActualUseCase,
    val myProviderProfile: ObtenerMiPerfilDeProveedorUseCase,
)

/**
 * The user profile (docs/domain/identity-flow.md): the signed-in User's name and photo
 * (`ActualizarPerfilDeUsuario`), sign-out (`CerrarSesion`) and the "Convertirme en Proveedor"
 * entry, which goes through the access guard (`RequerirCuentaVerificada`) — no session sends the
 * visitor to sign in, an unverified email to the verification notice. A visitor without a session
 * sees the profile screen too, with the sign-in / sign-up buttons. Account deletion is
 * [DeleteAccountViewModel]'s, hosted by the same screen.
 *
 * Same reuse rule as [SignInViewModel]: [ProfileScreen] sends [ProfileAction.Start] on every entry,
 * so after signing in elsewhere (or another account entirely) the screen never shows stale data.
 */
class ProfileViewModel(
    private val queries: ProfileQueries,
    private val actualizarPerfil: ActualizarPerfilDeUsuarioUseCase,
    private val cerrarSesion: CerrarSesionUseCase,
    private val requerirCuentaVerificada: RequerirCuentaVerificadaUseCase,
) : ViewModel() {

    private var loadJob: Job? = null

    private val _state = MutableStateFlow(ProfileUiState())
    val state: StateFlow<ProfileUiState> = _state.asStateFlow()

    private val _events = Channel<ProfileEvent>()
    val events = _events.receiveAsFlow()

    fun onAction(action: ProfileAction) {
        when (action) {
            ProfileAction.Start -> start()
            ProfileAction.EditClicked -> _state.update { it.startEditing() }
            ProfileAction.CancelEditClicked -> _state.update { it.copy(isEditing = false, nameError = null) }
            is ProfileAction.NameChanged -> _state.update { it.copy(nameInput = action.value, nameError = null) }
            is ProfileAction.PhotoUrlChanged -> _state.update { it.copy(photoUrlInput = action.value) }
            ProfileAction.SaveClicked -> save()
            ProfileAction.SignOutClicked -> signOut()
            ProfileAction.ProviderEntryClicked -> routeProviderEntry()
            ProfileAction.VerifyEmailClicked -> emit(ProfileEvent.NavigateToVerifyEmail)
            ProfileAction.SignInClicked -> emit(ProfileEvent.NavigateToSignIn)
            ProfileAction.SignUpClicked -> emit(ProfileEvent.NavigateToSignUp)
        }
    }

    private fun emit(event: ProfileEvent) {
        viewModelScope.launch { _events.send(event) }
    }

    private fun ProfileUiState.startEditing(): ProfileUiState {
        val current = user ?: return this
        return copy(
            isEditing = true,
            nameInput = current.displayName,
            photoUrlInput = current.photoUrl.orEmpty(),
            nameError = null,
            notice = null,
            actionError = null,
        )
    }

    /** Escenario: "Un visitante explora sin cuenta" — `null` is a visitor, shown as such. */
    private fun start() {
        loadJob?.cancel()
        _state.value = ProfileUiState()
        loadJob = viewModelScope.launch {
            when (val result = queries.currentUser()) {
                is DomainResult.Success -> showUser(result.value)
                is DomainResult.Failure -> _state.update { it.copy(isLoading = false, error = result.error) }
            }
        }
    }

    private suspend fun showUser(user: User?) {
        if (user == null) {
            _state.update { it.copy(isLoading = false, user = null) }
            return
        }
        when (val profile = queries.myProviderProfile()) {
            is DomainResult.Success ->
                _state.update { it.copy(isLoading = false, user = user, hasProviderProfile = profile.value != null) }
            is DomainResult.Failure -> _state.update { it.copy(isLoading = false, user = user, error = profile.error) }
        }
    }

    /** Escenario: "El Usuario actualiza su perfil". An empty name is a form error on the field. */
    private fun save() {
        val current = _state.value
        if (current.isSaving || !current.isEditing) return
        _state.update { it.copy(isSaving = true, nameError = null, actionError = null) }
        viewModelScope.launch {
            when (val result = actualizarPerfil(current.nameInput, current.photoUrlInput)) {
                is DomainResult.Success -> _state.update {
                    it.copy(isSaving = false, isEditing = false, user = result.value, notice = PROFILE_UPDATED_MESSAGE)
                }
                is DomainResult.Failure -> _state.update { it.failedToSave(result.error) }
            }
        }
    }

    private fun ProfileUiState.failedToSave(error: DomainError): ProfileUiState =
        if (error is DomainError.InvalidInput) {
            copy(isSaving = false, nameError = error.message)
        } else {
            copy(isSaving = false, actionError = error.describe())
        }

    /** Escenario: "El Usuario cierra sesión" — afterwards the app browses as a visitor. */
    private fun signOut() {
        if (_state.value.isSigningOut) return
        _state.update { it.copy(isSigningOut = true, actionError = null) }
        viewModelScope.launch {
            when (val result = cerrarSesion()) {
                is DomainResult.Success -> {
                    _state.update { it.copy(isSigningOut = false) }
                    _events.send(ProfileEvent.SignedOut)
                }
                is DomainResult.Failure ->
                    _state.update { it.copy(isSigningOut = false, actionError = result.error.describe()) }
            }
        }
    }

    /** Escenarios: "Un Usuario con correo sin verificar intenta ... activar el modo Proveedor" and the
     * visitor without a session. The guard answers `Unauthorized` (no session) or `InvalidInput`
     * (unverified email) — the only two ways it refuses. */
    private fun routeProviderEntry() {
        viewModelScope.launch {
            when (val result = requerirCuentaVerificada()) {
                is DomainResult.Success -> _events.send(ProfileEvent.NavigateToProviderMode)
                is DomainResult.Failure -> when (result.error) {
                    DomainError.Unauthorized -> _events.send(ProfileEvent.NavigateToSignIn)
                    is DomainError.InvalidInput -> _events.send(ProfileEvent.NavigateToVerifyEmail)
                    else -> _state.update { it.copy(actionError = result.error.describe()) }
                }
            }
        }
    }
}
