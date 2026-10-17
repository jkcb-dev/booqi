package corp.khin.solutions.booqi.feature.account

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign

/** The sign-in form: email + password, the social buttons and the way to the sign-up form. */
@Composable
internal fun SignInContent(state: SignInUiState, onAction: (SignInAction) -> Unit) {
    AccountScreenBody(
        title = "Iniciar sesión",
        trailing = { HeadingAction("Volver") { onAction(SignInAction.CloseClicked) } },
    ) {
        AccountTextField(
            value = state.email,
            onValueChange = { onAction(SignInAction.EmailChanged(it)) },
            label = "Correo",
            errorMessage = state.emailError,
            kind = AccountFieldKind.Email,
        )
        AccountTextField(
            value = state.password,
            onValueChange = { onAction(SignInAction.PasswordChanged(it)) },
            label = "Contraseña",
            errorMessage = state.passwordError,
            kind = AccountFieldKind.Password,
        )
        state.formError?.let { ErrorText(it) }
        Button(
            onClick = { onAction(SignInAction.SignInClicked) },
            enabled = !state.isSubmitting,
            modifier = Modifier.fillMaxWidth(),
        ) { Text(if (state.isSubmitting) "Ingresando..." else "Iniciar sesión") }
        SecondaryText("o", modifier = Modifier.fillMaxWidth(), align = TextAlign.Center)
        SocialSignInButtons(enabled = !state.isSubmitting) { onAction(SignInAction.SocialClicked(it)) }
        TextButton(onClick = { onAction(SignInAction.SwitchToSignUpClicked) }, modifier = Modifier.fillMaxWidth()) {
            Text("¿No tenés cuenta? Crear cuenta")
        }
    }
}
