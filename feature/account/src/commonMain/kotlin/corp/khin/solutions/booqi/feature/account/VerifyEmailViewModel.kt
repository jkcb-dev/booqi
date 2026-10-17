package corp.khin.solutions.booqi.feature.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import corp.khin.solutions.booqi.core.common.DomainError
import corp.khin.solutions.booqi.core.common.DomainResult
import corp.khin.solutions.booqi.domain.usecase.ObtenerUsuarioActualUseCase
import corp.khin.solutions.booqi.domain.usecase.ReenviarCorreoDeVerificacionUseCase
import corp.khin.solutions.booqi.domain.usecase.VerificarCorreoUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private val STILL_UNVERIFIED = DomainError.InvalidInput(
    "Todavía no verificamos tu correo. Abrí el enlace que te enviamos y volvé a intentar",
)
private const val RESENT_MESSAGE = "Te enviamos el correo de verificación otra vez"

/**
 * The notice an unverified email+password account sees before booking or activating Provider mode
 * (docs/domain/identity-flow.md — escenario "Un Usuario con correo sin verificar intenta
 * reservar"): where the email went, "Ya verifiqué mi correo" (`VerificarCorreo`) and "Reenviar
 * correo" (`ReenviarCorreoDeVerificacion`). Talks to the domain only through those use cases and
 * [ObtenerUsuarioActualUseCase].
 *
 * Same reuse rule as [SignInViewModel]: [VerifyEmailScreen] sends [VerifyEmailAction.Start] on
 * every entry. Starting on an account that is already verified (or on no session) is handled, not
 * assumed away.
 */
class VerifyEmailViewModel(
    private val obtenerUsuarioActual: ObtenerUsuarioActualUseCase,
    private val verificarCorreo: VerificarCorreoUseCase,
    private val reenviarCorreo: ReenviarCorreoDeVerificacionUseCase,
) : ViewModel() {

    private var loadJob: Job? = null

    private val _state = MutableStateFlow(VerifyEmailUiState())
    val state: StateFlow<VerifyEmailUiState> = _state.asStateFlow()

    private val _events = Channel<VerifyEmailEvent>()
    val events = _events.receiveAsFlow()

    fun onAction(action: VerifyEmailAction) {
        when (action) {
            VerifyEmailAction.Start -> start()
            VerifyEmailAction.ConfirmClicked -> confirm()
            VerifyEmailAction.ResendClicked -> resend()
            VerifyEmailAction.LaterClicked -> emit(VerifyEmailEvent.Close)
        }
    }

    private fun emit(event: VerifyEmailEvent) {
        viewModelScope.launch { _events.send(event) }
    }

    private fun start() {
        loadJob?.cancel()
        _state.value = VerifyEmailUiState()
        loadJob = viewModelScope.launch {
            when (val result = obtenerUsuarioActual()) {
                is DomainResult.Success -> {
                    val user = result.value
                    when {
                        user == null -> _state.update { it.copy(isLoading = false, error = DomainError.Unauthorized) }
                        user.isEmailVerified -> {
                            _state.update { it.copy(isLoading = false, email = user.email) }
                            _events.send(VerifyEmailEvent.Verified)
                        }
                        else -> _state.update { it.copy(isLoading = false, email = user.email) }
                    }
                }
                is DomainResult.Failure ->
                    _state.update { it.copy(isLoading = false, error = result.error) }
            }
        }
    }

    /** The User followed the link; ask the backend to refresh the account. Still unverified is an
     * expected answer (the link was not followed yet), not a failure. */
    private fun confirm() {
        if (_state.value.isChecking || _state.value.isLoading) return
        _state.update { it.copy(isChecking = true, notice = null, error = null) }
        viewModelScope.launch {
            when (val result = verificarCorreo()) {
                is DomainResult.Success ->
                    if (result.value.isEmailVerified) {
                        _state.update { it.copy(isChecking = false) }
                        _events.send(VerifyEmailEvent.Verified)
                    } else {
                        _state.update { it.copy(isChecking = false, error = STILL_UNVERIFIED) }
                    }
                is DomainResult.Failure ->
                    _state.update { it.copy(isChecking = false, error = result.error) }
            }
        }
    }

    /** Escenario: "...con opción de reenviar el correo de verificación". */
    private fun resend() {
        if (_state.value.isResending || _state.value.isLoading) return
        _state.update { it.copy(isResending = true, notice = null, error = null) }
        viewModelScope.launch {
            when (val result = reenviarCorreo()) {
                is DomainResult.Success -> _state.update { it.copy(isResending = false, notice = RESENT_MESSAGE) }
                is DomainResult.Failure -> _state.update { it.copy(isResending = false, error = result.error) }
            }
        }
    }
}
