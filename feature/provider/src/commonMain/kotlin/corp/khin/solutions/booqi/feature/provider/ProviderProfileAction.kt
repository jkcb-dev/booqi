package corp.khin.solutions.booqi.feature.provider

import kotlinx.datetime.LocalDate

/** User intents. The Composable only ever calls [ProviderProfileViewModel.onAction] with one of
 * these. */
sealed interface ProviderProfileAction {

    // P1 — activar modo Proveedor
    data object ActivateProviderMode : ProviderProfileAction

    // P2 — completar perfil
    data class NameChanged(val value: String) : ProviderProfileAction
    data class PhotoUrlChanged(val value: String) : ProviderProfileAction
    data class DescriptionChanged(val value: String) : ProviderProfileAction
    data class LocationChanged(val value: String) : ProviderProfileAction
    data object SaveProfile : ProviderProfileAction

    // P3 — pausar/reactivar perfil, reached in-screen once the profile is complete
    data object ShowPauseSheet : ProviderProfileAction
    data object DismissPauseSheet : ProviderProfileAction
    data class PauseFromChanged(val date: LocalDate) : ProviderProfileAction
    data class PauseUntilChanged(val date: LocalDate) : ProviderProfileAction
    data object ConfirmPause : ProviderProfileAction
    data object ReactivateProfile : ProviderProfileAction
}
