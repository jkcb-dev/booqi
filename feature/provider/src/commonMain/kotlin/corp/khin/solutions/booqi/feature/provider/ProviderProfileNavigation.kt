package corp.khin.solutions.booqi.feature.provider

/**
 * Where the Provider profile can lead, as one parameter object so the screen keeps a short
 * signature. Navigation is exposed as callbacks so Architect's wiring maps them to `Navigator`
 * calls without this module importing another feature: [onManageServices] ->
 * `Destination.ServiceList`, [onManageSchedule] -> `Destination.ScheduleManagement`,
 * [onManageBookings] -> `Destination.BookingRequestInbox`, [onSwitchToCustomer] -> back to the
 * Cliente shell.
 */
class ProviderProfileNavigation(
    val onManageServices: () -> Unit = {},
    val onManageSchedule: () -> Unit = {},
    val onManageBookings: () -> Unit = {},
    val onSwitchToCustomer: () -> Unit = {},
)
