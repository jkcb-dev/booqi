package corp.khin.solutions.booqi.feature.provider

import corp.khin.solutions.booqi.core.common.DomainError
import corp.khin.solutions.booqi.domain.model.Booking

/**
 * State of the received-ratings section on the completed profile (Figma P11). [reviews] are the
 * Provider's rated Bookings, newest first, as the use case returns them.
 *
 * [average] and [count] are derived from that same list rather than read from
 * `ProviderProfile.ratingAverage`/`ratingCount`: the persisted summary is by definition the mean
 * over exactly these Bookings, so they agree, and deriving them keeps the summary consistent with
 * the list shown and fresh on every entry (the profile ViewModel does not reload its profile).
 */
data class ProviderReviewsUiState(
    val isLoading: Boolean = true,
    val reviews: List<Booking> = emptyList(),
    val error: DomainError? = null,
) {
    private val stars: List<Int> get() = reviews.mapNotNull { it.rating?.stars }

    val count: Int get() = stars.size
    val average: Double? get() = stars.takeIf { it.isNotEmpty() }?.average()
}
