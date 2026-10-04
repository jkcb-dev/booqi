package corp.khin.solutions.booqi.data.di

import corp.khin.solutions.booqi.data.datasource.AvailabilityRemoteDataSource
import corp.khin.solutions.booqi.data.datasource.AuthRemoteDataSource
import corp.khin.solutions.booqi.data.datasource.BookingRemoteDataSource
import corp.khin.solutions.booqi.data.datasource.FakeAvailabilityRemoteDataSource
import corp.khin.solutions.booqi.data.datasource.FakeAuthRemoteDataSource
import corp.khin.solutions.booqi.data.datasource.FakeBookingRemoteDataSource
import corp.khin.solutions.booqi.data.datasource.FakeProviderProfileRemoteDataSource
import corp.khin.solutions.booqi.data.datasource.FakeServiceRemoteDataSource
import corp.khin.solutions.booqi.data.datasource.ProviderProfileRemoteDataSource
import corp.khin.solutions.booqi.data.datasource.SampleData
import corp.khin.solutions.booqi.data.datasource.ServiceRemoteDataSource
import corp.khin.solutions.booqi.data.repository.AvailabilityRepositoryImpl
import corp.khin.solutions.booqi.data.repository.BookingRepositoryImpl
import corp.khin.solutions.booqi.data.repository.CatalogRepositoryImpl
import corp.khin.solutions.booqi.data.repository.ProviderProfileRepositoryImpl
import corp.khin.solutions.booqi.data.repository.ServiceRepositoryImpl
import corp.khin.solutions.booqi.data.repository.UserRepositoryImpl
import corp.khin.solutions.booqi.domain.repository.AvailabilityRepository
import corp.khin.solutions.booqi.domain.repository.BookingRepository
import corp.khin.solutions.booqi.domain.repository.CatalogRepository
import corp.khin.solutions.booqi.domain.repository.ProviderProfileRepository
import corp.khin.solutions.booqi.domain.repository.ServiceRepository
import corp.khin.solutions.booqi.domain.repository.UserRepository
import org.koin.dsl.module

val dataModule = module {
    single<ProviderProfileRemoteDataSource> {
        FakeProviderProfileRemoteDataSource(seed = SampleData.providerProfiles())
    }
    single<ProviderProfileRepository> { ProviderProfileRepositoryImpl(get()) }
    single<ServiceRemoteDataSource> { FakeServiceRemoteDataSource(seed = SampleData.services()) }
    single<ServiceRepository> { ServiceRepositoryImpl(get()) }
    single<AvailabilityRemoteDataSource> { FakeAvailabilityRemoteDataSource() }
    single<AvailabilityRepository> { AvailabilityRepositoryImpl(get()) }
    single<CatalogRepository> { CatalogRepositoryImpl(get(), get()) }
    single<BookingRemoteDataSource> { FakeBookingRemoteDataSource(seed = SampleData.bookings()) }
    single<BookingRepository> { BookingRepositoryImpl(get()) }
    // Identity (#56): TEMPORARY in-memory auth until Supabase Auth (#27). Starts signed out.
    single<AuthRemoteDataSource> { FakeAuthRemoteDataSource() }
    single<UserRepository> { UserRepositoryImpl(get()) }
}
