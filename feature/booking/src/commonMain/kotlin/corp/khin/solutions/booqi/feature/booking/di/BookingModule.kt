package corp.khin.solutions.booqi.feature.booking.di

import corp.khin.solutions.booqi.feature.booking.AddressViewModel
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
 * Registered in `shared/.../di/InitKoin.kt`. The ViewModels take their `Clock` / `TimeZone`
 * defaults (`Clock.System`, the device's zone).
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
    viewModel {
        AddressViewModel(
            obtenerMiDireccion = get(),
            agregarDireccion = get(),
            actualizarDireccion = get(),
            buscarDireccion = get(),
        )
    }
    viewModel { BookingDetailViewModel(obtenerReserva = get(), cancelarReserva = get(), lookups = get()) }
    viewModel { MyBookingsViewModel(verHistorial = get(), lookups = get()) }
    viewModel { RateBookingViewModel(obtenerReserva = get(), calificarCita = get(), lookups = get()) }
}
