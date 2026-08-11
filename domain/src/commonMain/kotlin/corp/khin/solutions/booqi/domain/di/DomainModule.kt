package corp.khin.solutions.booqi.domain.di

import corp.khin.solutions.booqi.domain.usecase.GetFeaturedProvidersUseCase
import org.koin.dsl.module

val domainModule = module {
    factory { GetFeaturedProvidersUseCase(get()) }
}
