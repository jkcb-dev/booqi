package corp.khin.solutions.booqi.feature.provider.di

import corp.khin.solutions.booqi.feature.provider.BookingInboxCommands
import corp.khin.solutions.booqi.feature.provider.BookingInboxQueries
import corp.khin.solutions.booqi.feature.provider.BookingInboxViewModel
import corp.khin.solutions.booqi.feature.provider.CurrentProvider
import corp.khin.solutions.booqi.feature.provider.DateBlockingViewModel
import corp.khin.solutions.booqi.feature.provider.ProviderProfileViewModel
import corp.khin.solutions.booqi.feature.provider.ProviderReviewsViewModel
import corp.khin.solutions.booqi.feature.provider.ServiceEditorViewModel
import corp.khin.solutions.booqi.feature.provider.ServiceListViewModel
import corp.khin.solutions.booqi.feature.provider.WeeklyScheduleViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/**
 * Registered in `shared/.../di/InitKoin.kt`.
 *
 * [ServiceEditorViewModel] takes its `serviceId` (null = add) as the first Koin parameter —
 * `ServiceEditorScreen` supplies it via `parametersOf(serviceId)`.
 */
val providerModule = module {
    // The Provider screens act for the signed-in User's ProviderProfile: CurrentProvider resolves its
    // id through ObtenerMiPerfilDeProveedor every time it is needed.
    factory { CurrentProvider(obtenerMiPerfil = get()) }
    viewModel {
        ProviderProfileViewModel(
            activarModoProveedor = get(),
            completarPerfil = get(),
            pausarPerfil = get(),
            obtenerMiPerfil = get(),
            requerirCuentaVerificada = get(),
        )
    }
    viewModel {
        ServiceListViewModel(
            obtenerServicios = get(),
            deshabilitarServicio = get(),
            habilitarServicio = get(),
            currentProvider = get(),
        )
    }
    viewModel { params ->
        ServiceEditorViewModel(
            initialServiceId = params.getOrNull<String>(),
            agregarServicio = get(),
            editarServicio = get(),
            obtenerServicio = get(),
            currentProvider = get(),
        )
    }
    // Both halves of Destination.ScheduleManagement; ScheduleManagementScreen resolves them with
    // koinViewModel(). Their trailing clock/timeZone constructor arguments keep their defaults
    // (Clock.System, system time zone).
    viewModel {
        WeeklyScheduleViewModel(
            obtenerHorario = get(),
            definirHorario = get(),
            modificarHorario = get(),
            currentProvider = get(),
        )
    }
    viewModel {
        DateBlockingViewModel(
            obtenerHorario = get(),
            bloquearFechaHora = get(),
            desbloquearFechaHora = get(),
            currentProvider = get(),
        )
    }
    // Destination.BookingRequestInbox, and the P11 ratings section on the completed profile.
    // Their trailing clock/timeZone arguments keep their defaults (Clock.System, system time zone).
    viewModel {
        BookingInboxViewModel(
            queries = BookingInboxQueries(
                pendingRequests = get(),
                agenda = get(),
                booking = get(),
                services = get(),
                users = get(),
            ),
            commands = BookingInboxCommands(
                accept = get(),
                reject = get(),
                complete = get(),
                cancel = get(),
            ),
            currentProvider = get(),
        )
    }
    viewModel {
        ProviderReviewsViewModel(
            obtenerCalificaciones = get(),
            obtenerUsuarioPublico = get(),
            currentProvider = get(),
        )
    }
}
