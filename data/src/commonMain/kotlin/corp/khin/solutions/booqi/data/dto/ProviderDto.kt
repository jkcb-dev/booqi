package corp.khin.solutions.booqi.data.dto

/**
 * Wire/storage shape. Deliberately not `@Serializable` yet — no real backend contract exists to
 * shape it against. Add `kotlinx.serialization` once [corp.khin.solutions.booqi.data.datasource.
 * ProviderRemoteDataSource] has a real Ktor-backed implementation.
 */
data class ProviderDto(
    val id: String,
    val name: String,
    val category: String,
    val ratingOutOf5: Double,
    val priceFromCents: Int,
    val shortTagline: String,
)
