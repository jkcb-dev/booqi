package corp.khin.solutions.booqi.feature.browse

import corp.khin.solutions.booqi.core.common.DomainError

/**
 * What the access guard (`RequerirCuentaVerificada`) says about a "Reservar" tap: go on, or why not.
 * Browsing is public (docs/domain/identity-flow.md); only requesting a booking needs a verified
 * account, so C3 and C4 ask before handing over to the booking flow.
 */
internal sealed interface BookingAccess {
    data object Granted : BookingAccess

    /** No session: the visitor signs in or up first, then comes back to this screen. */
    data object SignInRequired : BookingAccess

    /** A session with an unverified email: the verification notice comes first. */
    data object VerificationRequired : BookingAccess

    /** The guard itself could not answer (storage or network failure). */
    data class Failed(val error: DomainError) : BookingAccess
}

/** The guard refuses in exactly two ways: `Unauthorized` (no session) and `InvalidInput` (unverified email). */
internal fun DomainError.toBookingAccess(): BookingAccess = when (this) {
    DomainError.Unauthorized -> BookingAccess.SignInRequired
    is DomainError.InvalidInput -> BookingAccess.VerificationRequired
    else -> BookingAccess.Failed(this)
}
