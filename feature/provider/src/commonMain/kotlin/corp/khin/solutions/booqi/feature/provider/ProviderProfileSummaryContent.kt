package corp.khin.solutions.booqi.feature.provider

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import corp.khin.solutions.booqi.core.designsystem.theme.BooqiCornerRadius
import corp.khin.solutions.booqi.core.designsystem.theme.BooqiSpacing
import corp.khin.solutions.booqi.core.designsystem.theme.LocalBooqiExtendedColors
import corp.khin.solutions.booqi.domain.model.ProviderProfile

/** Shown once [ProviderProfile.isComplete] is true — profile summary plus the P3 "pausar perfil"
 * action reached in-screen (see this PR's description for why it isn't a separate Destination). */
@Composable
internal fun ProfileSummaryContent(
    state: ProviderProfileUiState,
    onAction: (ProviderProfileAction) -> Unit,
    onManageServices: () -> Unit = {},
    onManageSchedule: () -> Unit = {},
) {
    val profile = state.profile ?: return
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(BooqiSpacing.md)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(BooqiSpacing.md),
    ) {
        Text("Tu perfil de Proveedor", style = MaterialTheme.typography.titleLarge)

        ProfileSummaryCard(profile)

        Button(onClick = onManageServices, modifier = Modifier.fillMaxWidth()) {
            Text("Mis servicios")
        }
        Button(onClick = onManageSchedule, modifier = Modifier.fillMaxWidth()) {
            Text("Mi horario")
        }

        if (profile.isPaused) {
            PausedBanner(profile)
            ReactivateButton(
                isPausing = state.isPausing,
                onClick = { onAction(ProviderProfileAction.ReactivateProfile) },
            )
        } else if (!state.isPauseSheetVisible) {
            OutlinedButton(
                onClick = { onAction(ProviderProfileAction.ShowPauseSheet) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Pausar perfil")
            }
        }

        if (state.isPauseSheetVisible) {
            PauseProfileSection(state = state, onAction = onAction)
        }
    }
}

@Composable
private fun ProfileSummaryCard(profile: ProviderProfile) {
    Card(shape = RoundedCornerShape(BooqiCornerRadius.medium)) {
        Column(
            modifier = Modifier.padding(BooqiSpacing.md),
            verticalArrangement = Arrangement.spacedBy(BooqiSpacing.xxs),
        ) {
            Text(profile.name.orEmpty(), style = MaterialTheme.typography.titleMedium)
            Text(
                text = profile.description.orEmpty(),
                style = MaterialTheme.typography.bodyMedium,
                color = LocalBooqiExtendedColors.current.ink2,
            )
            Text(
                text = profile.location.orEmpty(),
                style = MaterialTheme.typography.bodySmall,
                color = LocalBooqiExtendedColors.current.ink2,
            )
        }
    }
}

@Composable
private fun PausedBanner(profile: ProviderProfile) {
    Card(
        shape = RoundedCornerShape(BooqiCornerRadius.medium),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
    ) {
        Column(
            modifier = Modifier.padding(BooqiSpacing.md),
            verticalArrangement = Arrangement.spacedBy(BooqiSpacing.xxs),
        ) {
            Text(
                text = "Perfil pausado",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            profile.pausedRange?.let { range ->
                Text(
                    text = "Del ${range.start} al ${range.end}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
        }
    }
}

@Composable
private fun ReactivateButton(isPausing: Boolean, onClick: () -> Unit) {
    Button(onClick = onClick, enabled = !isPausing, modifier = Modifier.fillMaxWidth()) {
        Text(if (isPausing) "Reactivando..." else "Reactivar perfil")
    }
}
