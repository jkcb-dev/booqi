package corp.khin.solutions.booqi.data.mapper

import corp.khin.solutions.booqi.data.dto.UserDto
import corp.khin.solutions.booqi.domain.model.SocialProvider
import corp.khin.solutions.booqi.domain.model.User

fun UserDto.toDomain(): User = User(
    id = id,
    displayName = displayName,
    email = email,
    photoUrl = photoUrl,
    isEmailVerified = isEmailVerified,
    socialProvider = socialProvider?.toDomainSocialProvider(),
    isDeleted = isDeleted,
)

fun User.toDto(): UserDto = UserDto(
    id = id,
    displayName = displayName,
    email = email,
    photoUrl = photoUrl,
    isEmailVerified = isEmailVerified,
    socialProvider = socialProvider?.toStorageCode(),
    isDeleted = isDeleted,
)

// Explicit both ways (not enum names), like the Booking status mapping: the stored strings are the
// ones Supabase Auth uses for its providers, whatever the Kotlin entries are called.
fun SocialProvider.toStorageCode(): String = when (this) {
    SocialProvider.GOOGLE -> "google"
    SocialProvider.FACEBOOK -> "facebook"
    SocialProvider.APPLE -> "apple"
}

private fun String.toDomainSocialProvider(): SocialProvider = when (this) {
    "google" -> SocialProvider.GOOGLE
    "facebook" -> SocialProvider.FACEBOOK
    "apple" -> SocialProvider.APPLE
    else -> error("Unknown social provider: $this")
}
