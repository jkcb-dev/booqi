package corp.khin.solutions.booqi.domain.model

/**
 * An account (docs/DOMAIN.md — Identity bounded context, docs/domain/identity-flow.md). Plain data
 * class, no serialization annotations. `User.id` is the id every other aggregate references:
 * `Booking.customerId` and `ProviderProfile.userId` point here. A User may *also* own a
 * [ProviderProfile] (one optional per User) — but that profile has its own id, and
 * `Service`/`Availability`/`Booking`.`providerId` is **that** id, never [id] (see
 * "providerId contract" in docs/DOMAIN.md).
 *
 * - [displayName] is required at sign-up (it replaces the "Cliente" placeholder a Provider used to
 *   see). Social providers prefill it, and [photoUrl], from the provider's profile.
 * - [isEmailVerified]: email+password accounts start `false` and flip through
 *   [corp.khin.solutions.booqi.domain.usecase.VerificarCorreoUseCase]; social sign-ins start
 *   `true`. Only a verified account may request a booking or activate Provider mode — see
 *   [corp.khin.solutions.booqi.domain.usecase.RequerirCuentaVerificadaUseCase].
 * - [socialProvider] is `null` for an email+password account.
 * - [isDeleted]: the account was deleted. A deleted User is never removed — it is kept as an
 *   anonymized tombstone ([anonymized]) so completed Bookings and ratings that reference its id
 *   keep their history; it can no longer sign in. Use [publicName] to show it.
 */
data class User(
    val id: String,
    val displayName: String,
    val email: String?,
    val photoUrl: String? = null,
    val isEmailVerified: Boolean = false,
    val socialProvider: SocialProvider? = null,
    val isDeleted: Boolean = false,
) {
    /**
     * The name to show to *other* people (a Provider's inbox, a review): [DELETED_USER_NAME] for a
     * deleted account, [displayName] otherwise. This is the one place that rule lives — screens
     * must not hard-code "Usuario eliminado".
     */
    val publicName: String get() = if (isDeleted) DELETED_USER_NAME else displayName

    /** True when this account may request a booking or activate Provider mode. */
    val canBookAndOfferServices: Boolean get() = !isDeleted && isEmailVerified

    /** What other people may see of this account. */
    fun toPublic(): PublicUser = PublicUser(id, publicName, if (isDeleted) null else photoUrl, isDeleted)

    /**
     * The tombstone left by account deletion: name, photo, email and sign-in method erased,
     * [isDeleted] set. Only the [id] survives, so history keeps pointing at *something*.
     */
    fun anonymized(): User = User(
        id = id,
        displayName = "",
        email = null,
        photoUrl = null,
        isEmailVerified = false,
        socialProvider = null,
        isDeleted = true,
    )

    companion object {
        /** How a deleted account is shown to other people (docs/domain/identity-flow.md). */
        const val DELETED_USER_NAME = "Usuario eliminado"
    }
}

/** Social sign-in providers accepted by the app (docs/DATABASE.md — Supabase Auth). */
enum class SocialProvider { GOOGLE, FACEBOOK, APPLE }

/**
 * What another person may see of a [User]: name (already resolved through [User.publicName], so a
 * deleted account reads "Usuario eliminado") and photo — never the email or the verification state.
 */
data class PublicUser(
    val id: String,
    val displayName: String,
    val photoUrl: String?,
    val isDeleted: Boolean,
)
