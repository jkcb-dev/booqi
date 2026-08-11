package corp.khin.solutions.booqi.feature.browse.di

import corp.khin.solutions.booqi.feature.browse.BrowseViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val browseModule = module {
    viewModel { BrowseViewModel(get()) }
}
