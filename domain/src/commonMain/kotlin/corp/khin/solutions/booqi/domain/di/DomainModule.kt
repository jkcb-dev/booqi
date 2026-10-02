package corp.khin.solutions.booqi.domain.di

import corp.khin.solutions.booqi.domain.usecase.ActivarModoProveedorUseCase
import corp.khin.solutions.booqi.domain.usecase.AgregarServicioUseCase
import corp.khin.solutions.booqi.domain.usecase.CompletarPerfilDeProveedorUseCase
import corp.khin.solutions.booqi.domain.usecase.DeshabilitarServicioUseCase
import corp.khin.solutions.booqi.domain.usecase.EditarServicioUseCase
import corp.khin.solutions.booqi.domain.usecase.GetFeaturedProvidersUseCase
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
}
