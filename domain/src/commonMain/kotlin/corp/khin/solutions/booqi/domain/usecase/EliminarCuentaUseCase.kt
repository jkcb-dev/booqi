package corp.khin.solutions.booqi.domain.usecase

import corp.khin.solutions.booqi.core.common.DomainError
import corp.khin.solutions.booqi.core.common.DomainResult
import corp.khin.solutions.booqi.core.common.asFailure
import corp.khin.solutions.booqi.core.common.asSuccess
import corp.khin.solutions.booqi.core.common.flatMap
import corp.khin.solutions.booqi.core.common.map
import corp.khin.solutions.booqi.domain.model.AccountDeletionResult
import corp.khin.solutions.booqi.domain.model.ActiveBookings
import corp.khin.solutions.booqi.domain.model.Booking
import corp.khin.solutions.booqi.domain.model.BookingStatus
import corp.khin.solutions.booqi.domain.model.ProviderProfile
import corp.khin.solutions.booqi.domain.repository.BookingRepository
import corp.khin.solutions.booqi.domain.repository.ProviderProfileRepository
import corp.khin.solutions.booqi.domain.repository.UserRepository

/**
 * Escenarios: "El Usuario intenta eliminar su cuenta con reservas activas" / "...elimina su cuenta
 * sin reservas activas" (docs/domain/identity-flow.md). Account deletion is in V1 (App Store).
 *
 * **Blocked** while the user has a Booking that is `REQUESTED` or `CONFIRMED`
 * ([BookingStatus.isActive]) as Customer (`customerId == user.id`) **or** as Provider
 * (`providerId ==` the id of the user's ProviderProfile — the providerId contract, docs/DOMAIN.md).
 * Nothing is changed and the result is
 * [AccountDeletionResult.Blocked] carrying exactly which Bookings, by role, must be cancelled or
 * completed first. (A refusal that has to carry data is an outcome, not a `DomainError`.)
 *
 * **Otherwise the account is anonymized, not removed.** In this order, each step idempotent so a
 * failure part-way can simply be retried while the session is still alive:
 * 1. the personal data on the user's Customer-side Bookings — the delivery address snapshot and
 *    the free-text note — is erased; the Bookings themselves, their status and their **rating**
 *    stay, so the Provider's history and rating aggregate (`ratingAverage`/`ratingCount`, which
 *    this never recomputes) don't change;
 * 2. a ProviderProfile, if any, is anonymized and marked incomplete — the existing gate that
 *    removes it, and its Services, from Catalog search and public pages
 *    ([ProviderProfileRepository.anonymizeProfile]);
 * 3. the user becomes the [corp.khin.solutions.booqi.domain.model.User.anonymized] tombstone (name,
 *    photo, email erased, flagged deleted) and the session ends. People who had dealings with the
 *    user keep seeing a row,
 *    shown through `User.publicName` as "Usuario eliminado".
 *
 * `Unauthorized` without a session.
 */
class EliminarCuentaUseCase(
    private val users: UserRepository,
    private val bookings: BookingRepository,
    private val profiles: ProviderProfileRepository,
) {
    suspend operator fun invoke(): DomainResult<AccountDeletionResult> =
        users.getCurrentUser().flatMap { user ->
            if (user == null) return DomainError.Unauthorized.asFailure()
            profiles.findByUserId(user.id).flatMap { profile ->
                bookings.getBookingsByCustomer(user.id).flatMap { asCustomer ->
                    activeAsProvider(profile).flatMap { asProvider ->
                        val active = ActiveBookings(asCustomer.filter { it.status.isActive }, asProvider)
                        if (active.isEmpty) {
                            erase(profile, asCustomer)
                        } else {
                            AccountDeletionResult.Blocked(active).asSuccess()
                        }
                    }
                }
            }
        }

    private suspend fun activeAsProvider(profile: ProviderProfile?): DomainResult<List<Booking>> =
        if (profile == null) {
            emptyList<Booking>().asSuccess()
        } else {
            bookings.getBookingsByProvider(profile.id, ACTIVE_STATUSES)
        }

    private suspend fun erase(
        profile: ProviderProfile?,
        customerBookings: List<Booking>,
    ): DomainResult<AccountDeletionResult> =
        scrubPersonalData(customerBookings)
            .flatMap { if (profile == null) Unit.asSuccess() else profiles.anonymizeProfile(profile.id).map { } }
            .flatMap { users.deleteAccount() }
            .map { AccountDeletionResult.Deleted }

    private suspend fun scrubPersonalData(customerBookings: List<Booking>): DomainResult<Unit> {
        for (booking in customerBookings.filter { it.deliveryAddress != null || it.customerNote != null }) {
            val result = bookings.updateBooking(booking.copy(deliveryAddress = null, customerNote = null))
            if (result is DomainResult.Failure) return result
        }
        return Unit.asSuccess()
    }

    private companion object {
        val ACTIVE_STATUSES: Set<BookingStatus> = BookingStatus.entries.filter { it.isActive }.toSet()
    }
}
