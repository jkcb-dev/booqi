package corp.khin.solutions.booqi.data.datasource

import corp.khin.solutions.booqi.data.dto.AddressDto

/**
 * TEMPORARY in-memory address search until Google Places lands with the maps SDK (#24). Answers from
 * a fixed list of demo addresses in Buenos Aires (the same city as [SampleData]'s providers), matching
 * [query] as a case-insensitive substring of the line.
 */
class FakeAddressSearchRemoteDataSource(
    private val addresses: List<AddressDto> = DEMO_ADDRESSES,
) : AddressSearchRemoteDataSource {

    override suspend fun search(query: String): List<AddressDto> {
        val needle = query.trim()
        return if (needle.isEmpty()) emptyList() else addresses.filter { it.line.contains(needle, ignoreCase = true) }
    }

    companion object {
        val DEMO_ADDRESSES = listOf(
            AddressDto("Av. Corrientes 1234, CABA", -34.6037, -58.3816),
            AddressDto("Av. Santa Fe 3200, Palermo, CABA", -34.5880, -58.4100),
            AddressDto("Av. Callao 900, Recoleta, CABA", -34.5990, -58.3930),
            AddressDto("Av. Rivadavia 4500, Almagro, CABA", -34.6118, -58.4173),
            AddressDto("Av. Cabildo 2100, Belgrano, CABA", -34.5614, -58.4560),
            AddressDto("Defensa 700, San Telmo, CABA", -34.6155, -58.3722),
        )
    }
}
