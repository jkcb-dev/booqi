@file:OptIn(ExperimentalMaterial3Api::class)

package corp.khin.solutions.booqi.feature.provider

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.koin.compose.viewmodel.koinViewModel

/**
 * Covers P1 (activar modo) -> P2 (completar perfil) -> P3 (pausar perfil, in-screen action) as
 * one continuous flow on `Destination.ProviderProfileSetup`
 * (`corp.khin.solutions.booqi.core.navigation.Destination`), per docs/design/SCREENS.md's
 * confirmed structure for issue #13.
 */
@Suppress("ForbiddenComment") // Not surfacing errors visually yet is deliberately deferred
// scaffolding, not a bug — matches BrowseScreen's precedent (no snackbar host in
// core:designsystem yet).
@Composable
fun ProviderProfileScreen(
    viewModel: ProviderProfileViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()

    // Effects are collected once, separately from state — see ProviderProfileEvent for why.
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                ProviderProfileEvent.ProfileSaved,
                ProviderProfileEvent.ProfilePaused,
                ProviderProfileEvent.ProfileReactivated,
                -> Unit
                // TODO surface via snackbar once core:designsystem has one
                is ProviderProfileEvent.ShowError -> Unit
            }
        }
    }

    ProviderProfileContent(state = state, onAction = viewModel::onAction)
}

@Composable
private fun ProviderProfileContent(
    state: ProviderProfileUiState,
    onAction: (ProviderProfileAction) -> Unit,
) {
    val profile = state.profile
    when {
        state.isLoading -> LoadingContent()
        profile == null -> ActivateProviderModeContent(onAction = onAction)
        !profile.isComplete -> CompleteProfileContent(state = state, onAction = onAction)
        else -> ProfileSummaryContent(state = state, onAction = onAction)
    }
}

@Composable
private fun LoadingContent() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) { CircularProgressIndicator() }
}
