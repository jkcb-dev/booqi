package corp.khin.solutions.booqi.feature.provider

/**
 * One-shot effects — delivered via a Channel/Flow, never folded into [ProviderProfileUiState]. If
 * these lived in the state instead, they'd replay on every recomposition/process restore.
 */
sealed interface ProviderProfileEvent {
    data object ProfileSaved : ProviderProfileEvent
    data object ProfilePaused : ProviderProfileEvent
    data object ProfileReactivated : ProviderProfileEvent
    data class ShowError(val message: String) : ProviderProfileEvent
}
