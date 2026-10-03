package corp.khin.solutions.booqi.feature.provider

import corp.khin.solutions.booqi.domain.usecase.AceptarReservaUseCase
import corp.khin.solutions.booqi.domain.usecase.CancelarReservaAceptadaUseCase
import corp.khin.solutions.booqi.domain.usecase.CompletarCitaUseCase
import corp.khin.solutions.booqi.domain.usecase.ObtenerReservaUseCase
import corp.khin.solutions.booqi.domain.usecase.ObtenerReservasDelProveedorUseCase
import corp.khin.solutions.booqi.domain.usecase.ObtenerServiciosDelProveedorUseCase
import corp.khin.solutions.booqi.domain.usecase.ObtenerSolicitudesPendientesUseCase
import corp.khin.solutions.booqi.domain.usecase.RechazarReservaUseCase

/**
 * The read side of [BookingInboxViewModel], grouped into one parameter object so the ViewModel's
 * constructor stays under detekt's `LongParameterList` limit (this is a plain holder, not a layer:
 * the ViewModel still talks to the domain only through these use cases).
 * [services] resolves `Booking.serviceId` to a title — a Booking only holds the id.
 */
class BookingInboxQueries(
    val pendingRequests: ObtenerSolicitudesPendientesUseCase,
    val agenda: ObtenerReservasDelProveedorUseCase,
    val booking: ObtenerReservaUseCase,
    val services: ObtenerServiciosDelProveedorUseCase,
)

/** The write side of [BookingInboxViewModel]: the four Provider transitions of Grupo 4. */
class BookingInboxCommands(
    val accept: AceptarReservaUseCase,
    val reject: RechazarReservaUseCase,
    val complete: CompletarCitaUseCase,
    val cancel: CancelarReservaAceptadaUseCase,
)
