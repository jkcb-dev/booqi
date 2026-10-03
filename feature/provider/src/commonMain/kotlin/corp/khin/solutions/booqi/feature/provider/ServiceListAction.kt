package corp.khin.solutions.booqi.feature.provider

/** User intents on the service list. The Composable only ever calls
 * [ServiceListViewModel.onAction] with one of these. */
sealed interface ServiceListAction {
    data object Refresh : ServiceListAction

    /** The P4 per-service toggle: [enabled] is the state the Provider switched it **to**. */
    data class SetServiceEnabled(val serviceId: String, val enabled: Boolean) : ServiceListAction

    data object AddServiceClicked : ServiceListAction
    data class EditServiceClicked(val serviceId: String) : ServiceListAction
}
