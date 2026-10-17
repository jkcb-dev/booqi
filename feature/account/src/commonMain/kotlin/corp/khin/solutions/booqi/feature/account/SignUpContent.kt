package corp.khin.solutions.booqi.feature.account

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign

/** The sign-up form: name, email + password, the social buttons and the way to the sign-in form. */
@Composable
internal fun SignUpContent(state: SignUpUiState, onAction: (SignUpAction) -> Unit) {
    AccountScreenBody(
        title = "Crear cuenta",
        trailing = { HeadingAction("Volver") { onAction(SignUpAction.CloseClicked) } },
    ) {
        SecondaryText("Tu nombre es lo que verán los Proveedores cuando reserves con ellos.")
        AccountTextField(
            value = state.name,
            onValueChange = { onAction(SignUpAction.NameChanged(it)) },
            label = "Nombre",
            errorMessage = state.nameError,
        )
        AccountTextField(
            value = state.email,
            onValueChange = { onAction(SignUpAction.EmailChanged(it)) },
            label = "Correo",
            errorMessage = state.emailError,
            kind = AccountFieldKind.Email,
        )
        AccountTextField(
            value = state.password,
            onValueChange = { onAction(SignUpAction.PasswordChanged(it)) },
            label = "Contraseña",
            errorMessage = state.passwordError,
            kind = AccountFieldKind.Password,
        )
        state.formError?.let { ErrorText(it) }
        Button(
            onClick = { onAction(SignUpAction.SubmitClicked) },
            enabled = !state.isSubmitting,
            modifier = Modifier.fillMaxWidth(),
        ) { Text(if (state.isSubmitting) "Creando cuenta..." else "Crear cuenta") }
        SecondaryText("o", modifier = Modifier.fillMaxWidth(), align = TextAlign.Center)
        SocialSignInButtons(enabled = !state.isSubmitting) { onAction(SignUpAction.SocialClicked(it)) }
        TextButton(onClick = { onAction(SignUpAction.SwitchToSignInClicked) }, modifier = Modifier.fillMaxWidth()) {
            Text("¿Ya tenés cuenta? Iniciar sesión")
        }
    }
}
