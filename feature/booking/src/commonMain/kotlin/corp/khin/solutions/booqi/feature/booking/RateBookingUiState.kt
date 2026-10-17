package corp.khin.solutions.booqi.feature.booking

import corp.khin.solutions.booqi.core.common.DomainError
import corp.khin.solutions.booqi.domain.model.Booking
import corp.khin.solutions.booqi.domain.model.BookingStatus

/**
 * Immutable state of C12 (rate a completed appointment). [bookingId] is the Booking this state was
 * loaded *for* (the ViewModel instance is reused across visits). [stars] is 0 until one is chosen
 * (the domain accepts 1 to 5). [formError] carries a rejected submission's message (a
 * `DomainError.InvalidInput`, e.g. "Esta cita ya fue calificada") inline, never a crash.
 */
data class RateBookingUiState(
    val bookingId: String? = null,
    val isLoading: Boolean = true,
    val booking: Booking? = null,
    val serviceTitle: String? = null,
    val error: DomainError? = null,
    val stars: Int = 0,
    val comment: String = "",
    val isSubmitting: Boolean = false,
    val formError: String? = null,
) {
    /** C12 is only for a COMPLETED Booking that has no rating yet. */
    val isRatable: Boolean
        get() = booking?.let { it.status == BookingStatus.COMPLETED && it.rating == null } == true

    /** Why the Booking cannot be rated, when it cannot. */
    val notRatableReason: String?
        get() = booking?.takeUnless { isRatable }?.let {
            if (it.rating != null) "Esta cita ya fue calificada" else "Solo se puede calificar una cita completada"
        }

    val canSubmit: Boolean get() = isRatable && stars > 0 && !isSubmitting
}
