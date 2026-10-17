package corp.khin.solutions.booqi.feature.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import corp.khin.solutions.booqi.core.common.DomainError
import corp.khin.solutions.booqi.core.common.DomainResult
import corp.khin.solutions.booqi.domain.model.Registration
import corp.khin.solutions.booqi.domain.usecase.RegistrarUsuarioUseCase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * The sign-up form (docs/domain/identity-flow.md — `RegistrarUsuario`): name (required), email and
 * password, or one of the social providers (run against the fake until Platform Integration brings
 * the real SDKs). Talks to the domain only through [RegistrarUsuarioUseCase], whose `InvalidInput`
 * messages are the authority on what is valid and are shown on the field they are about.
 *
 * Same reuse rule as [SignInViewModel]: [SignUpScreen] sends [SignUpAction.Start] on every entry.
 */
class SignUpViewModel(
    private val registrarUsuario: RegistrarUsuarioUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(SignUpUiState())
    val state: StateFlow<SignUpUiState> = _state.asStateFlow()

    private val _events = Channel<SignUpEvent>()
    val events = _events.receiveAsFlow()

    fun onAction(action: SignUpAction) {
        when (action) {
            SignUpAction.Start -> _state.value = SignUpUiState()
            is SignUpAction.NameChanged ->
                _state.update { it.copy(name = action.value, nameError = null, formError = null) }
            is SignUpAction.EmailChanged ->
                _state.update { it.copy(email = action.value, emailError = null, formError = null) }
            is SignUpAction.PasswordChanged ->
                _state.update { it.copy(password = action.value, passwordError = null, formError = null) }
            SignUpAction.SubmitClicked -> submit(
                Registration.WithEmail(_state.value.name, _state.value.email, _state.value.password),
            )
            is SignUpAction.SocialClicked -> submit(Registration.WithSocial(action.provider))
            SignUpAction.SwitchToSignInClicked -> emit(SignUpEvent.SwitchToSignIn)
            SignUpAction.CloseClicked -> emit(SignUpEvent.Close)
        }
    }

    private fun emit(event: SignUpEvent) {
        viewModelScope.launch { _events.send(event) }
    }

    /** Escenarios: "Un visitante se registra con correo y contraseña" / "...con Google, Facebook o
     * Apple" / "El registro sin nombre se rechaza". Ignored while one is in flight. */
    private fun submit(registration: Registration) {
        if (_state.value.isSubmitting) return
        _state.update {
            it.copy(isSubmitting = true, nameError = null, emailError = null, passwordError = null, formError = null)
        }
        viewModelScope.launch {
            when (val result = registrarUsuario(registration)) {
                is DomainResult.Success -> {
                    // The password never outlives the attempt that used it.
                    _state.update { it.copy(isSubmitting = false, password = "") }
                    _events.send(SignUpEvent.Authenticated(needsEmailVerification = !result.value.isEmailVerified))
                }
                is DomainResult.Failure -> _state.update { it.failedWith(result.error) }
            }
        }
    }

    private fun SignUpUiState.failedWith(error: DomainError): SignUpUiState {
        val message = error.describe()
        val field = (error as? DomainError.InvalidInput)?.let { fieldOf(message) }
        return copy(
            isSubmitting = false,
            nameError = message.takeIf { field == AuthFormField.NAME },
            emailError = message.takeIf { field == AuthFormField.EMAIL },
            passwordError = message.takeIf { field == AuthFormField.PASSWORD },
            formError = message.takeIf { field == null },
        )
    }
}
