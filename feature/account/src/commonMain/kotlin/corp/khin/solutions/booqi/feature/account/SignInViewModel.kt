package corp.khin.solutions.booqi.feature.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import corp.khin.solutions.booqi.core.common.DomainError
import corp.khin.solutions.booqi.core.common.DomainResult
import corp.khin.solutions.booqi.domain.model.Credentials
import corp.khin.solutions.booqi.domain.usecase.IniciarSesionUseCase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val WRONG_CREDENTIALS_MESSAGE = "Correo o contraseña incorrectos"

/**
 * The sign-in form (docs/domain/identity-flow.md — `IniciarSesion`). Talks to the domain only
 * through [IniciarSesionUseCase]; the social buttons run the same use case against the fake
 * (real Google/Facebook/Apple SDKs belong to Platform Integration).
 *
 * **No init-time reset.** Our navigator does not scope ViewModels (docs/DEVELOPMENT.md), so this
 * instance is reused on every visit: [SignInScreen] sends [SignInAction.Start] on each entry, which
 * wipes a previous visit's email, password and errors.
 */
class SignInViewModel(
    private val iniciarSesion: IniciarSesionUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(SignInUiState())
    val state: StateFlow<SignInUiState> = _state.asStateFlow()

    private val _events = Channel<SignInEvent>()
    val events = _events.receiveAsFlow()

    fun onAction(action: SignInAction) {
        when (action) {
            SignInAction.Start -> _state.value = SignInUiState()
            is SignInAction.EmailChanged ->
                _state.update { it.copy(email = action.value, emailError = null, formError = null) }
            is SignInAction.PasswordChanged ->
                _state.update { it.copy(password = action.value, passwordError = null, formError = null) }
            SignInAction.SignInClicked ->
                submit(Credentials.WithEmail(_state.value.email, _state.value.password))
            is SignInAction.SocialClicked -> submit(Credentials.WithSocial(action.provider))
            SignInAction.SwitchToSignUpClicked -> emit(SignInEvent.SwitchToSignUp)
            SignInAction.CloseClicked -> emit(SignInEvent.Close)
        }
    }

    private fun emit(event: SignInEvent) {
        viewModelScope.launch { _events.send(event) }
    }

    /** Escenario: "El Usuario inicia sesión" (and its social twin). Ignored while one is in flight. */
    private fun submit(credentials: Credentials) {
        if (_state.value.isSubmitting) return
        _state.update { it.copy(isSubmitting = true, emailError = null, passwordError = null, formError = null) }
        viewModelScope.launch {
            when (val result = iniciarSesion(credentials)) {
                is DomainResult.Success -> {
                    // The password never outlives the attempt that used it.
                    _state.update { it.copy(isSubmitting = false, password = "") }
                    _events.send(SignInEvent.Authenticated(needsEmailVerification = false))
                }
                is DomainResult.Failure -> _state.update { it.failedWith(result.error) }
            }
        }
    }

    private fun SignInUiState.failedWith(error: DomainError): SignInUiState {
        val message = error.describe(unauthorized = WRONG_CREDENTIALS_MESSAGE)
        val field = (error as? DomainError.InvalidInput)?.let { fieldOf(message) }
        return copy(
            isSubmitting = false,
            emailError = message.takeIf { field == AuthFormField.EMAIL },
            passwordError = message.takeIf { field == AuthFormField.PASSWORD },
            formError = message.takeIf { field == null || field == AuthFormField.NAME },
        )
    }
}
