package corp.khin.solutions.booqi.data.di

import corp.khin.solutions.booqi.data.datasource.AvailabilityRemoteDataSource
import corp.khin.solutions.booqi.data.datasource.BookingRemoteDataSource
import corp.khin.solutions.booqi.data.datasource.FakeAvailabilityRemoteDataSource
import corp.khin.solutions.booqi.data.datasource.FakeBookingRemoteDataSource
import corp.khin.solutions.booqi.data.datasource.FakeProviderProfileRemoteDataSource
import corp.khin.solutions.booqi.data.datasource.FakeProviderRemoteDataSource
import corp.khin.solutions.booqi.data.datasource.FakeServiceRemoteDataSource
import corp.khin.solutions.booqi.data.datasource.ProviderProfileRemoteDataSource
import corp.khin.solutions.booqi.data.datasource.ProviderRemoteDataSource
import corp.khin.solutions.booqi.data.datasource.SampleData
import corp.khin.solutions.booqi.data.datasource.ServiceRemoteDataSource
import corp.khin.solutions.booqi.data.repository.AvailabilityRepositoryImpl
import corp.khin.solutions.booqi.data.repository.BookingRepositoryImpl
import corp.khin.solutions.booqi.data.repository.ProviderProfileRepositoryImpl
import corp.khin.solutions.booqi.data.repository.ServiceCatalogRepositoryImpl
import corp.khin.solutions.booqi.data.repository.ServiceRepositoryImpl
import corp.khin.solutions.booqi.domain.repository.AvailabilityRepository
import corp.khin.solutions.booqi.domain.repository.BookingRepository
import corp.khin.solutions.booqi.domain.repository.ProviderProfileRepository
import corp.khin.solutions.booqi.domain.repository.ServiceCatalogRepository
import corp.khin.solutions.booqi.domain.repository.ServiceRepository
import org.koin.dsl.module

val dataModule = module {
    single<ProviderRemoteDataSource> { FakeProviderRemoteDataSource() }
    single<ServiceCatalogRepository> { ServiceCatalogRepositoryImpl(get()) }
    single<ProviderProfileRemoteDataSource> { FakeProviderProfileRemoteDataSource() }
    single<ProviderProfileRepository> { ProviderProfileRepositoryImpl(get()) }
    single<ServiceRemoteDataSource> { FakeServiceRemoteDataSource(seed = SampleData.services()) }
    single<ServiceRepository> { ServiceRepositoryImpl(get()) }
    single<AvailabilityRemoteDataSource> { FakeAvailabilityRemoteDataSource() }
    single<AvailabilityRepository> { AvailabilityRepositoryImpl(get()) }
    single<BookingRemoteDataSource> { FakeBookingRemoteDataSource(seed = SampleData.bookings()) }
    single<BookingRepository> { BookingRepositoryImpl(get()) }
}
