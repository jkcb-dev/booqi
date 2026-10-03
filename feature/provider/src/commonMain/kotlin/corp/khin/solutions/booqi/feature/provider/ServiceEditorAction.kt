package corp.khin.solutions.booqi.feature.provider

import corp.khin.solutions.booqi.domain.model.ServiceModality

/** User intents on the add/edit service form. */
sealed interface ServiceEditorAction {
    /**
     * Sent by [ServiceEditorScreen] every time it enters composition (and when its `serviceId`
     * changes): resets the form to a clean state for [serviceId] (`null` = add) and, when
     * editing, loads that service. Needed because ViewModels are not destination-scoped (see
     * docs/DEVELOPMENT.md), so a reused instance would otherwise show the previous visit's form.
     */
    data class Start(val serviceId: String?) : ServiceEditorAction

    data class PhotoUrlChanged(val value: String) : ServiceEditorAction
    data class TitleChanged(val value: String) : ServiceEditorAction
    data class DescriptionChanged(val value: String) : ServiceEditorAction
    data class PriceChanged(val value: String) : ServiceEditorAction
    data class DurationChanged(val value: String) : ServiceEditorAction
    data class ModalityChanged(val modality: ServiceModality) : ServiceEditorAction
    data object Save : ServiceEditorAction
}
