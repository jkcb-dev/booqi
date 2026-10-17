package corp.khin.solutions.booqi.feature.account

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import org.koin.compose.viewmodel.koinViewModel

/**
 * The user profile, for `Destination.Profile`
 * (`corp.khin.solutions.booqi.core.navigation.Destination`): name, photo, sign-out, account
 * deletion and the "Convertirme en Proveedor" entry — or, for a visitor without a session, the
 * invitation to sign in or sign up. Reloads on every entry.
 */
@Composable
fun ProfileScreen(
    navigation: ProfileNavigation,
    viewModel: ProfileViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val currentNavigation by rememberUpdatedState(navigation)

    // Reload on every entry: the ViewModel instance is reused across visits, and the account may
    // have changed (signed in, verified, deleted) since the last one.
    LaunchedEffect(Unit) { viewModel.onAction(ProfileAction.Start) }

    // Effects are collected once, separately from state — see ProfileEvent for why.
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                ProfileEvent.NavigateToProviderMode -> currentNavigation.onProviderMode()
                ProfileEvent.NavigateToSignIn -> currentNavigation.onSignInRequired()
                ProfileEvent.NavigateToSignUp -> currentNavigation.onSignUpRequested()
                ProfileEvent.NavigateToVerifyEmail -> currentNavigation.onVerificationRequired()
                ProfileEvent.SignedOut -> currentNavigation.onSessionEnded()
            }
        }
    }

    ProfileContent(
        state = state,
        onAction = viewModel::onAction,
        deleteAccountSection = { DeleteAccountSection(onAccountDeleted = { currentNavigation.onSessionEnded() }) },
    )
}
