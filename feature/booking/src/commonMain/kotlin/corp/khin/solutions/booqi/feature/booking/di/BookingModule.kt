package corp.khin.solutions.booqi.feature.booking.di

import corp.khin.solutions.booqi.feature.booking.BookingCustomerAccess
import corp.khin.solutions.booqi.feature.booking.BookingDetailViewModel
import corp.khin.solutions.booqi.feature.booking.BookingFlowViewModel
import corp.khin.solutions.booqi.feature.booking.BookingLookups
import corp.khin.solutions.booqi.feature.booking.BookingSlotQueries
import corp.khin.solutions.booqi.feature.booking.MyBookingsViewModel
import corp.khin.solutions.booqi.feature.booking.RateBookingViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/**
 * Not registered in `shared/.../di/InitKoin.kt` yet — that is Architect's (see "For Architect" in
 * the #26 PR). The ViewModels take their `Clock` / `TimeZone` defaults (`Clock.System`, the
 * device's zone).
 */
val bookingModule = module {
    factory { BookingLookups(services = get(), profiles = get()) }
    viewModel {
        BookingFlowViewModel(
            slots = BookingSlotQueries(detail = get(), schedule = get(), generate = get(), available = get()),
            customer = BookingCustomerAccess(requireVerifiedAccount = get(), savedAddress = get()),
            solicitarReserva = get(),
        )
    }
    viewModel { BookingDetailViewModel(obtenerReserva = get(), cancelarReserva = get(), lookups = get()) }
    viewModel { MyBookingsViewModel(verHistorial = get(), lookups = get()) }
    viewModel { RateBookingViewModel(obtenerReserva = get(), calificarCita = get(), lookups = get()) }
}
