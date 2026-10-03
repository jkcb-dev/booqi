package corp.khin.solutions.booqi.domain.di

import corp.khin.solutions.booqi.domain.usecase.ActivarModoProveedorUseCase
import corp.khin.solutions.booqi.domain.usecase.AgregarServicioUseCase
import corp.khin.solutions.booqi.domain.usecase.BloquearFechaHoraUseCase
import corp.khin.solutions.booqi.domain.usecase.CompletarPerfilDeProveedorUseCase
import corp.khin.solutions.booqi.domain.usecase.DefinirHorarioSemanalUseCase
import corp.khin.solutions.booqi.domain.usecase.DesbloquearFechaHoraUseCase
import corp.khin.solutions.booqi.domain.usecase.DeshabilitarServicioUseCase
import corp.khin.solutions.booqi.domain.usecase.EditarServicioUseCase
import corp.khin.solutions.booqi.domain.usecase.GenerarTimeSlotsUseCase
import corp.khin.solutions.booqi.domain.usecase.GetFeaturedProvidersUseCase
import corp.khin.solutions.booqi.domain.usecase.HabilitarServicioUseCase
import corp.khin.solutions.booqi.domain.usecase.ModificarHorarioSemanalUseCase
import corp.khin.solutions.booqi.domain.usecase.ObtenerHorarioUseCase
import corp.khin.solutions.booqi.domain.usecase.ObtenerServicioUseCase
import corp.khin.solutions.booqi.domain.usecase.ObtenerServiciosDelProveedorUseCase
import corp.khin.solutions.booqi.domain.usecase.PausarPerfilUseCase
import org.koin.dsl.module

val domainModule = module {
    factory { GetFeaturedProvidersUseCase(get()) }
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
}
