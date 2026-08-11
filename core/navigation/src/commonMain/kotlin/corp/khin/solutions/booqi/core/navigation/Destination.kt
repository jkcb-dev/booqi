package corp.khin.solutions.booqi.core.navigation

/**
 * Every screen in the app, in one closed set owned by this module — not by any `feature:*`
 * module. Feature modules depend on [Navigator] (and reference destinations *other* features own
 * only through this sealed type), so `feature:browse` never has a compile-time dependency on
 * `feature:booking`'s screen classes.
 */
sealed interface Destination {
    data object Browse : Destination
    data class ProviderDetail(val providerId: String) : Destination
    data class Booking(val providerId: String) : Destination
    data object BookingConfirmation : Destination
    data object MyBookings : Destination
}
