package corp.khin.solutions.booqi.feature.provider.di

import corp.khin.solutions.booqi.feature.provider.BookingInboxCommands
import corp.khin.solutions.booqi.feature.provider.BookingInboxQueries
import corp.khin.solutions.booqi.feature.provider.BookingInboxViewModel
import corp.khin.solutions.booqi.feature.provider.DateBlockingViewModel
import corp.khin.solutions.booqi.feature.provider.ProviderProfileViewModel
import corp.khin.solutions.booqi.feature.provider.ProviderReviewsViewModel
import corp.khin.solutions.booqi.feature.provider.ServiceEditorViewModel
import corp.khin.solutions.booqi.feature.provider.ServiceListViewModel
import corp.khin.solutions.booqi.feature.provider.WeeklyScheduleViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/**
 * Not yet registered in `shared/.../di/InitKoin.kt` — that's Architect's exclusive chokepoint
 * file (see this PR's description). Follows the exact `browseModule` pattern so registering it is
 * a one-line addition to `initKoin`'s `modules(...)` list.
 *
 * [ServiceEditorViewModel] takes its `serviceId` (null = add) as the first Koin parameter —
 * `ServiceEditorScreen` supplies it via `parametersOf(serviceId)`. The trailing `providerId`
 * constructor argument keeps its TEMPORARY default.
 */
val providerModule = module {
    viewModel { ProviderProfileViewModel(get(), get(), get()) }
    viewModel {
        ServiceListViewModel(
            obtenerServicios = get(),
            deshabilitarServicio = get(),
            habilitarServicio = get(),
        )
    }
    viewModel { params ->
        ServiceEditorViewModel(
            initialServiceId = params.getOrNull<String>(),
            agregarServicio = get(),
            editarServicio = get(),
            obtenerServicio = get(),
        )
    }
    // Both halves of Destination.ScheduleManagement; ScheduleManagementScreen resolves them with
    // koinViewModel(). Their trailing providerId/clock/timeZone constructor arguments keep their
    // defaults (TEMPORARY provider id, Clock.System, system time zone).
    viewModel {
        WeeklyScheduleViewModel(
            obtenerHorario = get(),
            definirHorario = get(),
            modificarHorario = get(),
        )
    }
    viewModel {
        DateBlockingViewModel(
            obtenerHorario = get(),
            bloquearFechaHora = get(),
            desbloquearFechaHora = get(),
        )
    }
    // Destination.BookingRequestInbox, and the P11 ratings section on the completed profile.
    // Their trailing clock/timeZone/providerId arguments keep their defaults (SystemClock, system
    // time zone, TEMPORARY provider id).
    viewModel {
        BookingInboxViewModel(
            queries = BookingInboxQueries(
                pendingRequests = get(),
                agenda = get(),
                booking = get(),
                services = get(),
            ),
            commands = BookingInboxCommands(
                accept = get(),
                reject = get(),
                complete = get(),
                cancel = get(),
            ),
        )
    }
    viewModel { ProviderReviewsViewModel(obtenerCalificaciones = get()) }
}
