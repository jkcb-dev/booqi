package corp.khin.solutions.booqi.data.di

import corp.khin.solutions.booqi.data.datasource.AddressSearchRemoteDataSource
import corp.khin.solutions.booqi.data.datasource.AuthRemoteDataSource
import corp.khin.solutions.booqi.data.datasource.AvailabilityRemoteDataSource
import corp.khin.solutions.booqi.data.datasource.BookingRemoteDataSource
import corp.khin.solutions.booqi.data.datasource.FakeAddressSearchRemoteDataSource
import corp.khin.solutions.booqi.data.datasource.FakeAuthRemoteDataSource
import corp.khin.solutions.booqi.data.datasource.FakeAvailabilityRemoteDataSource
import corp.khin.solutions.booqi.data.datasource.FakeBookingRemoteDataSource
import corp.khin.solutions.booqi.data.datasource.FakeProviderProfileRemoteDataSource
import corp.khin.solutions.booqi.data.datasource.FakeServiceRemoteDataSource
import corp.khin.solutions.booqi.data.datasource.FakeUserProfileRemoteDataSource
import corp.khin.solutions.booqi.data.datasource.InMemoryAccountStore
import corp.khin.solutions.booqi.data.datasource.ProviderProfileRemoteDataSource
import corp.khin.solutions.booqi.data.datasource.SampleData
import corp.khin.solutions.booqi.data.datasource.ServiceRemoteDataSource
import corp.khin.solutions.booqi.data.datasource.UserProfileRemoteDataSource
import corp.khin.solutions.booqi.data.repository.AddressSearchRepositoryImpl
import corp.khin.solutions.booqi.data.repository.AvailabilityRepositoryImpl
import corp.khin.solutions.booqi.data.repository.BookingRepositoryImpl
import corp.khin.solutions.booqi.data.repository.CatalogRepositoryImpl
import corp.khin.solutions.booqi.data.repository.ProviderProfileRepositoryImpl
import corp.khin.solutions.booqi.data.repository.ServiceRepositoryImpl
import corp.khin.solutions.booqi.data.repository.SessionRepositoryImpl
import corp.khin.solutions.booqi.data.repository.UserProfileRepositoryImpl
import corp.khin.solutions.booqi.domain.repository.AddressSearchRepository
import corp.khin.solutions.booqi.domain.repository.AvailabilityRepository
import corp.khin.solutions.booqi.domain.repository.BookingRepository
import corp.khin.solutions.booqi.domain.repository.CatalogRepository
import corp.khin.solutions.booqi.domain.repository.ProviderProfileRepository
import corp.khin.solutions.booqi.domain.repository.ServiceRepository
import corp.khin.solutions.booqi.domain.repository.SessionRepository
import corp.khin.solutions.booqi.domain.repository.UserProfileRepository
import org.koin.dsl.module

val dataModule = module {
    single<ProviderProfileRemoteDataSource> {
        FakeProviderProfileRemoteDataSource(seed = SampleData.providerProfiles())
    }
    single<ProviderProfileRepository> { ProviderProfileRepositoryImpl(get()) }
    single<ServiceRemoteDataSource> { FakeServiceRemoteDataSource(seed = SampleData.services()) }
    single<ServiceRepository> { ServiceRepositoryImpl(get()) }
    single<AvailabilityRemoteDataSource> { FakeAvailabilityRemoteDataSource(seed = SampleData.weeklyHours()) }
    single<AvailabilityRepository> { AvailabilityRepositoryImpl(get()) }
    single<CatalogRepository> { CatalogRepositoryImpl(get(), get()) }
    single<BookingRemoteDataSource> { FakeBookingRemoteDataSource(seed = SampleData.bookings()) }
    single<BookingRepository> { BookingRepositoryImpl(get()) }
    // Identity (#56): TEMPORARY in-memory auth until Supabase Auth (#27). Starts signed out; the seeded
    // accounts include the demo account (see SampleData).
    // One shared in-memory store behind both fakes, so the accounts and the session exist once.
    single { InMemoryAccountStore(seed = SampleData.accounts()) }
    single<AuthRemoteDataSource> { FakeAuthRemoteDataSource(get()) }
    single<UserProfileRemoteDataSource> { FakeUserProfileRemoteDataSource(get()) }
    single<SessionRepository> { SessionRepositoryImpl(get()) }
    single<UserProfileRepository> { UserProfileRepositoryImpl(get(), get()) }
    // Address search (#23): TEMPORARY in-memory list until Google Places (#24).
    single<AddressSearchRemoteDataSource> { FakeAddressSearchRemoteDataSource() }
    single<AddressSearchRepository> { AddressSearchRepositoryImpl(get()) }
}
