package corp.khin.solutions.booqi.domain.model

/**
 * How a visitor creates an account (docs/domain/identity-flow.md). A parameter object, not a long
 * argument list, and it matches what the sign-up screen submits.
 */
sealed interface Registration {

    /** Email + password; [name] is required. A verification email is sent afterwards. */
    data class WithEmail(val name: String, val email: String, val password: String) : Registration {
        // The password must never end up in a log line or a crash report.
        override fun toString(): String = "WithEmail(name=$name, email=$email, password=***)"
    }

    /**
     * Google / Facebook / Apple. The provider's own consent screen runs on the platform and hands
     * back the account's name, photo and a verified email — nothing else is asked of the visitor.
     */
    data class WithSocial(val provider: SocialProvider) : Registration
}

/** How an existing User signs in; the counterpart of [Registration]. */
sealed interface Credentials {

    data class WithEmail(val email: String, val password: String) : Credentials {
        override fun toString(): String = "WithEmail(email=$email, password=***)"
    }

    data class WithSocial(val provider: SocialProvider) : Credentials
}
