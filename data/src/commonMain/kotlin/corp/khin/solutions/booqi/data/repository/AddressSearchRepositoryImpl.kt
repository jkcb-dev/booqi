package corp.khin.solutions.booqi.data.repository

import corp.khin.solutions.booqi.core.common.DomainResult
import corp.khin.solutions.booqi.core.common.asSuccess
import corp.khin.solutions.booqi.data.datasource.AddressSearchRemoteDataSource
import corp.khin.solutions.booqi.domain.model.Address
import corp.khin.solutions.booqi.domain.repository.AddressSearchRepository

class AddressSearchRepositoryImpl(
    private val remoteDataSource: AddressSearchRemoteDataSource,
) : AddressSearchRepository {

    override suspend fun search(query: String): DomainResult<List<Address>> = guarded {
        remoteDataSource.search(query).map { Address(it.line, it.latitude, it.longitude) }.asSuccess()
    }
}
