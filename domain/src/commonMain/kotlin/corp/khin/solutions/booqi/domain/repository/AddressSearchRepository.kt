package corp.khin.solutions.booqi.domain.repository

import corp.khin.solutions.booqi.core.common.DomainResult
import corp.khin.solutions.booqi.domain.model.Address

/**
 * Address lookup by free text ("busca su dirección en Google Maps", docs/domain/customer-flow.md
 * § Grupo 2). Every candidate carries its map position, so it can be saved as is. Picking a point on
 * a map instead of searching is #24 (Google Places / maps SDK); until then the data layer answers
 * from a TEMPORARY in-memory list.
 */
interface AddressSearchRepository {
    /** Addresses matching [query], best first; empty when nothing matches. */
    suspend fun search(query: String): DomainResult<List<Address>>
}
