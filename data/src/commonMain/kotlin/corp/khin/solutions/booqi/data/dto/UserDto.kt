package corp.khin.solutions.booqi.data.dto

/**
 * Storage shape of a `profiles` row joined with its Supabase Auth identity (docs/DATABASE.md).
 * Deliberately not `@Serializable` yet — same reasoning as [ProviderProfileDto] (#27).
 *
 * [socialProvider] is `null` for an email+password account, otherwise `"google"`, `"facebook"` or
 * `"apple"`. [email] is `null` on an anonymized (deleted) account, whose [displayName] is `""`.
 * The saved address is the three nullable `address_line`/`address_lat`/`address_lng` columns, set
 * together or not at all. Never holds a password — credentials belong to the auth backend, not to this row.
 */
data class UserDto(
    val id: String,
    val displayName: String,
    val email: String?,
    val photoUrl: String?,
    val isEmailVerified: Boolean,
    val socialProvider: String?,
    val isDeleted: Boolean,
    val addressLine: String? = null,
    val addressLat: Double? = null,
    val addressLng: Double? = null,
)
