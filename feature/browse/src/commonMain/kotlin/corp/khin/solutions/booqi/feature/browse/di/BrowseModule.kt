package corp.khin.solutions.booqi.feature.browse.di

import corp.khin.solutions.booqi.feature.browse.BrowseViewModel
import corp.khin.solutions.booqi.feature.browse.ProviderPublicProfileViewModel
import corp.khin.solutions.booqi.feature.browse.ServiceDetailViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/**
 * Already registered in `shared/.../di/InitKoin.kt`. [BrowseViewModel]'s trailing `customerLocation`
 * argument keeps its TEMPORARY default until platform location exists (#24).
 */
val browseModule = module {
    viewModel { BrowseViewModel(buscarServicios = get()) }
    viewModel { ServiceDetailViewModel(verDetalleServicio = get()) }
    viewModel { ProviderPublicProfileViewModel(verPerfilProveedor = get()) }
}
