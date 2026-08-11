package corp.khin.solutions.booqi.di

import corp.khin.solutions.booqi.data.di.dataModule
import corp.khin.solutions.booqi.domain.di.domainModule
import corp.khin.solutions.booqi.feature.browse.di.browseModule
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module

/**
 * Single Koin entry point, called once per process from each platform's actual entry point
 * (androidApp's MainActivity, shared's iosMain MainViewController). [platformModule] is where
 * each platform supplies whatever it alone can provide (e.g. Android's application Context) —
 * everything platform-agnostic is listed here so adding a feature module is a one-line change in
 * one place, not a change in every platform entry point.
 */
fun initKoin(platformModule: Module = module {}, appDeclaration: KoinAppDeclaration = {}) {
    startKoin {
        appDeclaration()
        modules(
            domainModule,
            dataModule,
            browseModule,
            platformModule,
        )
    }
}
