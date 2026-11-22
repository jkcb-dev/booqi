package corp.khin.solutions.booqi

import androidx.compose.runtime.Composable
import corp.khin.solutions.booqi.core.navigation.Destination
import corp.khin.solutions.booqi.core.navigation.Navigator
import corp.khin.solutions.booqi.feature.booking.AddressScreen
import corp.khin.solutions.booqi.feature.booking.BookingDetailScreen
import corp.khin.solutions.booqi.feature.booking.BookingFlowScreen
import corp.khin.solutions.booqi.feature.booking.MyBookingsScreen
import corp.khin.solutions.booqi.feature.booking.RateBookingScreen

/**
 * `feature:booking` — the Customer's reservations. [Destination.Auth] is pushed over the screen that
 * needed an account, so after signing in the visitor is back on the same `Booking(serviceId)` (the
 * flow resumes only when re-entered for that Service) or on "Mis reservas". [Destination.AddressSelection]
 * is pushed over the booking flow or the profile and shows no bottom bar.
 */
@Composable
internal fun BookingDestination(destination: Destination, navigator: Navigator) {
    when (destination) {
        is Destination.Booking -> BookingFlowScreen(
            serviceId = destination.serviceId,
            onSignInRequired = { navigator.navigateTo(Destination.Auth()) },
            // Pushed over the flow: back returns to the same Service, which resumes the chosen slot
            // and re-reads the saved address.
            onAddAddress = { navigator.navigateTo(Destination.AddressSelection) },
            // Replace the flow with the new booking's detail; back then returns to the Service.
            onDone = { id ->
                navigator.navigateBack()
                navigator.navigateTo(Destination.BookingDetail(id))
            },
            onFinished = { navigator.backOr(Destination.Browse) },
        )
        is Destination.BookingDetail -> BookingDetailScreen(
            bookingId = destination.bookingId,
            onRebook = { serviceId -> navigator.navigateTo(Destination.Booking(serviceId)) },
            onGoHome = { navigator.navigateToRoot(Destination.Browse) },
            onRate = { id -> navigator.navigateTo(Destination.RateBooking(id)) },
            onFinished = { navigator.backOr(Destination.MyBookings) },
        )
        is Destination.MyBookings -> MyBookingsScreen(
            onBookingSelected = { id -> navigator.navigateTo(Destination.BookingDetail(id)) },
            onSignInRequired = { navigator.navigateTo(Destination.Auth()) },
            onExplore = { navigator.navigateToRoot(Destination.Browse) },
        )
        is Destination.RateBooking -> RateBookingScreen(
            bookingId = destination.bookingId,
            onFinished = { navigator.backOr(Destination.MyBookings) },
        )
        // Pushed from the booking flow or the profile: saving or backing out is a plain back.
        is Destination.AddressSelection -> AddressScreen(
            onFinished = { navigator.backOr(Destination.Profile) },
        )
        else -> Unit
    }
}
