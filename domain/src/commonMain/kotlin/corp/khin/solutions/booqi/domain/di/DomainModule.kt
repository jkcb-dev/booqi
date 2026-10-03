package corp.khin.solutions.booqi.domain.di

import corp.khin.solutions.booqi.domain.usecase.AceptarReservaUseCase
import corp.khin.solutions.booqi.domain.usecase.ActivarModoProveedorUseCase
import corp.khin.solutions.booqi.domain.usecase.AgregarServicioUseCase
import corp.khin.solutions.booqi.domain.usecase.BloquearFechaHoraUseCase
import corp.khin.solutions.booqi.domain.usecase.BuscarServiciosUseCase
import corp.khin.solutions.booqi.domain.usecase.CalificarCitaUseCase
import corp.khin.solutions.booqi.domain.usecase.CancelarReservaAceptadaUseCase
import corp.khin.solutions.booqi.domain.usecase.CompletarCitaUseCase
import corp.khin.solutions.booqi.domain.usecase.CompletarPerfilDeProveedorUseCase
import corp.khin.solutions.booqi.domain.usecase.DefinirHorarioSemanalUseCase
import corp.khin.solutions.booqi.domain.usecase.DesbloquearFechaHoraUseCase
import corp.khin.solutions.booqi.domain.usecase.DeshabilitarServicioUseCase
import corp.khin.solutions.booqi.domain.usecase.EditarServicioUseCase
import corp.khin.solutions.booqi.domain.usecase.ExpirarSolicitudesVencidasUseCase
import corp.khin.solutions.booqi.domain.usecase.GenerarTimeSlotsUseCase
import corp.khin.solutions.booqi.domain.usecase.HabilitarServicioUseCase
import corp.khin.solutions.booqi.domain.usecase.ModificarHorarioSemanalUseCase
import corp.khin.solutions.booqi.domain.usecase.ObtenerCalificacionesDelProveedorUseCase
import corp.khin.solutions.booqi.domain.usecase.ObtenerHorarioUseCase
import corp.khin.solutions.booqi.domain.usecase.ObtenerReservaUseCase
import corp.khin.solutions.booqi.domain.usecase.ObtenerReservasDelProveedorUseCase
import corp.khin.solutions.booqi.domain.usecase.ObtenerServicioUseCase
import corp.khin.solutions.booqi.domain.usecase.ObtenerServiciosDelProveedorUseCase
import corp.khin.solutions.booqi.domain.usecase.ObtenerSolicitudesPendientesUseCase
import corp.khin.solutions.booqi.domain.usecase.ObtenerTimeSlotsDisponiblesUseCase
import corp.khin.solutions.booqi.domain.usecase.PausarPerfilUseCase
import corp.khin.solutions.booqi.domain.usecase.RecalcularCalificacionDelProveedorUseCase
import corp.khin.solutions.booqi.domain.usecase.RechazarReservaUseCase
import corp.khin.solutions.booqi.domain.usecase.SolicitarReservaUseCase
import corp.khin.solutions.booqi.domain.usecase.VerDetalleServicioUseCase
import corp.khin.solutions.booqi.domain.usecase.VerPerfilProveedorUseCase
import org.koin.dsl.module

val domainModule = module {
    factory { ActivarModoProveedorUseCase(get()) }
    factory { CompletarPerfilDeProveedorUseCase(get()) }
    factory { PausarPerfilUseCase(get()) }
    factory { AgregarServicioUseCase(get()) }
    factory { EditarServicioUseCase(get()) }
    factory { DeshabilitarServicioUseCase(get()) }
    factory { HabilitarServicioUseCase(get()) }
    factory { ObtenerServicioUseCase(get()) }
    factory { ObtenerServiciosDelProveedorUseCase(get()) }
    factory { DefinirHorarioSemanalUseCase(get()) }
    factory { ModificarHorarioSemanalUseCase(get()) }
    factory { BloquearFechaHoraUseCase(get()) }
    factory { DesbloquearFechaHoraUseCase(get()) }
    factory { ObtenerHorarioUseCase(get()) }
    factory { GenerarTimeSlotsUseCase() }
    // Scheduling / Booking (Grupo 4, #18). Use cases taking a Clock use its Clock.System default.
    factory { AceptarReservaUseCase(get()) }
    factory { RechazarReservaUseCase(get()) }
    factory { CompletarCitaUseCase(get()) }
    factory { CancelarReservaAceptadaUseCase(get()) }
    factory { ExpirarSolicitudesVencidasUseCase(get()) }
    factory { RecalcularCalificacionDelProveedorUseCase(get(), get()) }
    factory { CalificarCitaUseCase(get(), get()) }
    factory { ObtenerTimeSlotsDisponiblesUseCase(get(), get(), get(), get()) }
    factory { SolicitarReservaUseCase(get(), get(), get()) }
    factory { ObtenerSolicitudesPendientesUseCase(get()) }
    factory { ObtenerReservaUseCase(get()) }
    factory { ObtenerReservasDelProveedorUseCase(get()) }
    factory { ObtenerCalificacionesDelProveedorUseCase(get()) }
    // Catalog (Cliente · Búsqueda, #20). BuscarServicios uses its Clock.System/system-zone defaults.
    factory { BuscarServiciosUseCase(get()) }
    factory { VerDetalleServicioUseCase(get(), get()) }
    factory { VerPerfilProveedorUseCase(get(), get(), get()) }
}
