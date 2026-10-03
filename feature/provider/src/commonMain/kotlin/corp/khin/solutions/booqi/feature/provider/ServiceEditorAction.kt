package corp.khin.solutions.booqi.feature.provider

import corp.khin.solutions.booqi.domain.model.ServiceModality

/** User intents on the add/edit service form. */
sealed interface ServiceEditorAction {
    data class PhotoUrlChanged(val value: String) : ServiceEditorAction
    data class TitleChanged(val value: String) : ServiceEditorAction
    data class DescriptionChanged(val value: String) : ServiceEditorAction
    data class PriceChanged(val value: String) : ServiceEditorAction
    data class DurationChanged(val value: String) : ServiceEditorAction
    data class ModalityChanged(val modality: ServiceModality) : ServiceEditorAction
    data object Save : ServiceEditorAction
}
