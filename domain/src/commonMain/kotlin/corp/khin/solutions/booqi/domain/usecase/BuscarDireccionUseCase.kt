package corp.khin.solutions.booqi.domain.usecase

import corp.khin.solutions.booqi.core.common.DomainResult
import corp.khin.solutions.booqi.core.common.asSuccess
import corp.khin.solutions.booqi.domain.model.Address
import corp.khin.solutions.booqi.domain.repository.AddressSearchRepository

/**
 * Query `BuscarDireccion` (docs/domain/customer-flow.md § Grupo 2): the Customer types part of
 * their address and gets candidate [Address]es to pick from. The query is trimmed; one shorter than
 * [MIN_QUERY_LENGTH] characters answers an empty list without calling the repository (too vague to
 * search, and it spares a remote call per keystroke).
 */
class BuscarDireccionUseCase(
    private val search: AddressSearchRepository,
) {
    suspend operator fun invoke(query: String): DomainResult<List<Address>> {
        val trimmed = query.trim()
        return if (trimmed.length < MIN_QUERY_LENGTH) emptyList<Address>().asSuccess() else search.search(trimmed)
    }

    companion object {
        const val MIN_QUERY_LENGTH = 3
    }
}
