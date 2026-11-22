package corp.khin.solutions.booqi.data.repository

import corp.khin.solutions.booqi.core.common.DomainError
import corp.khin.solutions.booqi.core.common.DomainResult
import corp.khin.solutions.booqi.data.datasource.AddressSearchRemoteDataSource
import corp.khin.solutions.booqi.data.datasource.FakeAddressSearchRemoteDataSource
import corp.khin.solutions.booqi.data.dto.AddressDto
import corp.khin.solutions.booqi.domain.model.Address
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AddressSearchRepositoryImplTest {

    private val repository = AddressSearchRepositoryImpl(FakeAddressSearchRemoteDataSource())

    private suspend fun found(query: String) = (repository.search(query) as DomainResult.Success).value

    @Test
    fun `a match is case-insensitive and keeps the coordinates`() = runTest {
        val results = found("corrientes")

        assertEquals(listOf(Address("Av. Corrientes 1234, CABA", -34.6037, -58.3816)), results)
    }

    @Test
    fun `a substring of several addresses returns all of them`() = runTest {
        assertTrue(found("CABA").size > 1)
    }

    @Test
    fun `no match is an empty list`() = runTest {
        assertEquals(emptyList(), found("zzzz"))
    }

    @Test
    fun `every demo address has a valid position`() {
        FakeAddressSearchRemoteDataSource.DEMO_ADDRESSES.forEach {
            assertTrue(Address(it.line, it.latitude, it.longitude).point.isValid, it.line)
        }
    }

    @Test
    fun `a datasource exception becomes a domain error`() = runTest {
        val failing = AddressSearchRepositoryImpl(object : AddressSearchRemoteDataSource {
            override suspend fun search(query: String): List<AddressDto> = error("boom")
        })

        val result = failing.search("abc")

        assertEquals(DomainError.Unknown("boom"), (result as DomainResult.Failure).error)
    }
}
