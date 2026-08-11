package corp.khin.solutions.booqi.data.di

import corp.khin.solutions.booqi.data.datasource.FakeProviderRemoteDataSource
import corp.khin.solutions.booqi.data.datasource.ProviderRemoteDataSource
import corp.khin.solutions.booqi.data.repository.ServiceCatalogRepositoryImpl
import corp.khin.solutions.booqi.domain.repository.ServiceCatalogRepository
import org.koin.dsl.module

val dataModule = module {
    single<ProviderRemoteDataSource> { FakeProviderRemoteDataSource() }
    single<ServiceCatalogRepository> { ServiceCatalogRepositoryImpl(get()) }
}
