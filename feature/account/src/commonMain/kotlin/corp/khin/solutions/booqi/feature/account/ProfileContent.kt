package corp.khin.solutions.booqi.feature.account

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import corp.khin.solutions.booqi.core.designsystem.theme.BooqiCornerRadius
import corp.khin.solutions.booqi.core.designsystem.theme.BooqiSpacing
import corp.khin.solutions.booqi.domain.model.User

private const val HEADING = "Tu perfil"

/**
 * The profile under its heading, which every state keeps: loading, a failed load, a visitor
 * without a session, and the signed-in User. [deleteAccountSection] is the account-deletion flow,
 * a slot because it has its own ViewModel.
 */
@Composable
internal fun ProfileContent(
    state: ProfileUiState,
    onAction: (ProfileAction) -> Unit,
    deleteAccountSection: @Composable () -> Unit,
) {
    AccountScreenBody(title = HEADING) {
        val user = state.user
        when {
            state.isLoading -> LoadingIndicator()
            state.error != null && user == null ->
                LoadFailure(state.error.describe(), onRetry = { onAction(ProfileAction.Start) })
            user == null -> VisitorContent(onAction)
            else -> SignedInContent(state, user, onAction, deleteAccountSection)
        }
    }
}

@Composable
private fun VisitorContent(onAction: (ProfileAction) -> Unit) {
    SecondaryText("Iniciá sesión o creá una cuenta para reservar servicios y gestionar tu perfil.")
    Button(onClick = { onAction(ProfileAction.SignInClicked) }, modifier = Modifier.fillMaxWidth()) {
        Text("Iniciar sesión")
    }
    OutlinedButton(onClick = { onAction(ProfileAction.SignUpClicked) }, modifier = Modifier.fillMaxWidth()) {
        Text("Crear cuenta")
    }
    ProviderEntryCard(hasProviderProfile = false, onClick = { onAction(ProfileAction.ProviderEntryClicked) })
}

@Composable
private fun SignedInContent(
    state: ProfileUiState,
    user: User,
    onAction: (ProfileAction) -> Unit,
    deleteAccount: @Composable () -> Unit,
) {
    state.notice?.let {
        Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
    }
    state.error?.let { ErrorText(it.describe()) }
    state.actionError?.let { ErrorText(it) }
    if (state.isEditing) {
        ProfileEditForm(state = state, onAction = onAction)
    } else {
        UserSummary(user = user, onEdit = { onAction(ProfileAction.EditClicked) })
    }
    if (!user.isEmailVerified) {
        VerifyEmailCard(onClick = { onAction(ProfileAction.VerifyEmailClicked) })
    }
    ProviderEntryCard(
        hasProviderProfile = state.hasProviderProfile,
        onClick = { onAction(ProfileAction.ProviderEntryClicked) },
    )
    OutlinedButton(
        onClick = { onAction(ProfileAction.SignOutClicked) },
        enabled = !state.isSigningOut,
        modifier = Modifier.fillMaxWidth(),
    ) { Text(if (state.isSigningOut) "Cerrando sesión..." else "Cerrar sesión") }
    deleteAccount()
}

/** Avatar, name and email, with the way to edit them. */
@Composable
private fun UserSummary(user: User, onEdit: () -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(BooqiSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        UserAvatar(name = user.displayName)
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(BooqiSpacing.xxs)) {
            Text(user.displayName, style = MaterialTheme.typography.titleMedium)
            user.email?.let { SecondaryText(it) }
        }
    }
    OutlinedButton(onClick = onEdit, modifier = Modifier.fillMaxWidth()) { Text("Editar perfil") }
}

@Composable
private fun VerifyEmailCard(onClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(BooqiCornerRadius.medium),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
    ) {
        Column(
            modifier = Modifier.padding(BooqiSpacing.md),
            verticalArrangement = Arrangement.spacedBy(BooqiSpacing.xs),
        ) {
            Text(
                text = "Tu correo todavía no está verificado",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            Text(
                text = "Lo necesitás para reservar o convertirte en Proveedor.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            OutlinedButton(onClick = onClick) { Text("Verificar correo") }
        }
    }
}

/** The entry to Provider mode. It is shown to visitors too: tapping it without a session leads to
 * sign-in, and back here afterwards (identity-flow.md: an account is needed to activate it). */
@Composable
private fun ProviderEntryCard(hasProviderProfile: Boolean, onClick: () -> Unit) {
    Card(shape = RoundedCornerShape(BooqiCornerRadius.medium), modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(BooqiSpacing.md),
            verticalArrangement = Arrangement.spacedBy(BooqiSpacing.xs),
        ) {
            Text(
                text = if (hasProviderProfile) "Modo Proveedor" else "¿Ofrecés servicios?",
                style = MaterialTheme.typography.titleSmall,
            )
            SecondaryText(
                if (hasProviderProfile) {
                    "Gestioná tu perfil, tus servicios, tu horario y tus solicitudes."
                } else {
                    "Activá el modo Proveedor para aparecer en las búsquedas y recibir reservas."
                },
            )
            Button(onClick = onClick) {
                Text(if (hasProviderProfile) "Ir a mi panel de Proveedor" else "Convertirme en Proveedor")
            }
        }
    }
}
