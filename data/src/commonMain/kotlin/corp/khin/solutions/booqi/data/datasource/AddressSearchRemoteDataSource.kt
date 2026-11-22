package corp.khin.solutions.booqi.data.datasource

import corp.khin.solutions.booqi.data.dto.AddressDto

/** Source of address candidates for a free-text query. `data`-only; the DTO never reaches `domain`. */
interface AddressSearchRemoteDataSource {
    /** Candidates for [query], best first; empty when nothing matches. */
    suspend fun search(query: String): List<AddressDto>
}
