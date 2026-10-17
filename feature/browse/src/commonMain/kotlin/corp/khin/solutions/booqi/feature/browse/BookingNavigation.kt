package corp.khin.solutions.booqi.feature.browse

/**
 * Where a "Reservar" tap can lead, as one parameter object so the screens keep short signatures.
 * Navigation is exposed as callbacks so Architect's wiring maps them to `Navigator` calls without
 * this module importing another feature. [onBook] fires only once the access guard passed (with
 * the Service and its Provider; the booking flow itself is #26); [onSignInRequired] opens the
 * sign-in flow over the current screen and [onVerificationRequired] the email-verification notice —
 * finishing either returns to the screen that asked.
 */
class BookingNavigation(
    val onBook: (serviceId: String, providerId: String) -> Unit,
    val onSignInRequired: () -> Unit,
    val onVerificationRequired: () -> Unit,
)
