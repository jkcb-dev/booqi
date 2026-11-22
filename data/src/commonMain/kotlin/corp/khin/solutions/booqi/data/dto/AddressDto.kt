package corp.khin.solutions.booqi.data.dto

/** One address candidate as the search source returns it: a display line and its map position. */
data class AddressDto(
    val line: String,
    val latitude: Double,
    val longitude: Double,
)
