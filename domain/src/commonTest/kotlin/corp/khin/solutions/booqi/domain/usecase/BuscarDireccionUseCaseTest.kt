package corp.khin.solutions.booqi.domain.usecase

import corp.khin.solutions.booqi.core.common.DomainError
import corp.khin.solutions.booqi.core.common.DomainResult
import corp.khin.solutions.booqi.core.common.asFailure
import corp.khin.solutions.booqi.core.common.asSuccess
import corp.khin.solutions.booqi.domain.model.Address
import corp.khin.solutions.booqi.domain.repository.AddressSearchRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

/** `BuscarDireccion` (customer-flow.md § Grupo 2): the search path of the address picker. */
class BuscarDireccionUseCaseTest {

    private val casa = Address("Av. Corrientes 1234, Buenos Aires", -34.6037, -58.3816)
    private val repository = RecordingSearch(listOf(casa))
    private val buscar = BuscarDireccionUseCase(repository)

    @Test
    fun `a query is trimmed and answered by the repository`() = runTest {
        val result = buscar("  Corrientes  ")

        assertEquals(listOf(casa), (result as DomainResult.Success).value)
        assertEquals(listOf("Corrientes"), repository.queries)
    }

    @Test
    fun `a query shorter than three characters is empty without calling the repository`() = runTest {
        assertEquals(emptyList(), (buscar("ab") as DomainResult.Success).value)
        assertEquals(emptyList(), (buscar("  a  ") as DomainResult.Success).value)
        assertEquals(emptyList(), (buscar("") as DomainResult.Success).value)

        assertEquals(emptyList(), repository.queries)
    }

    @Test
    fun `a query of exactly three characters is searched`() = runTest {
        buscar("abc")

        assertEquals(listOf("abc"), repository.queries)
    }

    @Test
    fun `a repository failure is passed on`() = runTest {
        repository.failure = DomainError.NoConnection

        val result = buscar("Corrientes")

        assertIs<DomainResult.Failure>(result)
        assertEquals(DomainError.NoConnection, result.error)
    }

    private class RecordingSearch(private val answer: List<Address>) : AddressSearchRepository {
        val queries = mutableListOf<String>()
        var failure: DomainError? = null

        override suspend fun search(query: String): DomainResult<List<Address>> {
            queries += query
            return failure?.asFailure() ?: answer.asSuccess()
        }
    }
}
