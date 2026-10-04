package corp.khin.solutions.booqi.data.dto

/**
 * Storage shape of a `profiles` row joined with its Supabase Auth identity (docs/DATABASE.md).
 * Deliberately not `@Serializable` yet — same reasoning as [ProviderProfileDto] (#27).
 *
 * [socialProvider] is `null` for an email+password account, otherwise `"google"`, `"facebook"` or
 * `"apple"`. [email] is `null` on an anonymized (deleted) account, whose [displayName] is `""`.
 * Never holds a password — credentials belong to the auth backend, not to this row.
 */
data class UserDto(
    val id: String,
    val displayName: String,
    val email: String?,
    val photoUrl: String?,
    val isEmailVerified: Boolean,
    val socialProvider: String?,
    val isDeleted: Boolean,
)
