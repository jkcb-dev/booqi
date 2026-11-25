package corp.khin.solutions.booqi.domain.repository

import corp.khin.solutions.booqi.core.common.DomainResult
import corp.khin.solutions.booqi.domain.model.Address

/**
 * Address lookup by free text ("busca su dirección en Google Maps", docs/domain/customer-flow.md
 * § Grupo 2). Every candidate carries its map position, so it can be saved as is. The map-pin path
 * is #24 (`AddressMapPicker` + platform reverse geocode); this port is still the TEMPORARY
 * in-memory demo list until Google Places (or equivalent) replaces it in `data`.
 */
interface AddressSearchRepository {
    /** Addresses matching [query], best first; empty when nothing matches. */
    suspend fun search(query: String): DomainResult<List<Address>>
}
