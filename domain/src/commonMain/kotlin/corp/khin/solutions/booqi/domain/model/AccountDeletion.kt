package corp.khin.solutions.booqi.domain.model

/**
 * The Bookings that stop an account from being deleted: those still `REQUESTED` or `CONFIRMED`
 * ([BookingStatus.isActive]), listed by the role the user plays in them. [asCustomer] are Bookings
 * the user requested (`customerId == user.id`); [asProvider] are Bookings received on the user's
 * ProviderProfile (`providerId == ProviderProfile.id`). The user must cancel or complete each one.
 */
data class ActiveBookings(
    val asCustomer: List<Booking>,
    val asProvider: List<Booking>,
) {
    val isEmpty: Boolean get() = asCustomer.isEmpty() && asProvider.isEmpty()
}

/**
 * Outcome of [corp.khin.solutions.booqi.domain.usecase.EliminarCuentaUseCase]. Being refused
 * because of active bookings is an expected business answer that has to carry data, so it is an
 * outcome here rather than a [corp.khin.solutions.booqi.core.common.DomainError]; a `Failure`
 * from the use case means something actually went wrong.
 */
sealed interface AccountDeletionResult {
    /** The account was anonymized and the session ended. */
    data object Deleted : AccountDeletionResult

    /** Nothing was changed: [activeBookings] must be cancelled or completed first. */
    data class Blocked(val activeBookings: ActiveBookings) : AccountDeletionResult
}
