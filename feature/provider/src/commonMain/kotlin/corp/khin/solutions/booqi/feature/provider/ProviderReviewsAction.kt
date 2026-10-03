package corp.khin.solutions.booqi.feature.provider

/** User intents on the received-ratings section. */
sealed interface ProviderReviewsAction {
    /** Sent every time the section enters composition, and by "Reintentar". */
    data object Start : ProviderReviewsAction
}
