package corp.khin.solutions.booqi.feature.provider.di

import corp.khin.solutions.booqi.feature.provider.ProviderProfileViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/**
 * Not yet registered in `shared/.../di/InitKoin.kt` — that's Architect's exclusive chokepoint
 * file (see this PR's description). Follows the exact `browseModule` pattern so registering it is
 * a one-line addition to `initKoin`'s `modules(...)` list.
 */
val providerModule = module {
    viewModel { ProviderProfileViewModel(get(), get(), get()) }
}
