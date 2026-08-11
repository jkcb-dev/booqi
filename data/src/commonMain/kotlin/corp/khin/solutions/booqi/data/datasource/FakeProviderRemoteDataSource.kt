package corp.khin.solutions.booqi.data.datasource

import corp.khin.solutions.booqi.data.dto.ProviderDto

/**
 * TEMPORARY. Stands in for a real Ktor-backed [ProviderRemoteDataSource] until there's an actual
 * backend API to call — see [corp.khin.solutions.booqi.core.network.createHttpClient]. This
 * exists purely so the module graph and MVI wiring can be proven end-to-end (Architect's
 * definition of done) without waiting on a backend decision. Replace, don't extend.
 */
class FakeProviderRemoteDataSource : ProviderRemoteDataSource {
    override suspend fun fetchFeaturedProviders(): List<ProviderDto> = listOf(
        ProviderDto(
            id = "1",
            name = "Jane's Nails",
            category = "NAILS",
            ratingOutOf5 = 4.8,
            priceFromCents = 3500,
            shortTagline = "Gel & acrylic specialist",
        ),
        ProviderDto(
            id = "2",
            name = "Marco's Barber Shop",
            category = "BARBER",
            ratingOutOf5 = 4.6,
            priceFromCents = 2500,
            shortTagline = "Classic cuts & fades",
        ),
        ProviderDto(
            id = "3",
            name = "QuickFix Technicians",
            category = "TECHNICIAN",
            ratingOutOf5 = 4.4,
            priceFromCents = 4500,
            shortTagline = "Same-day appliance repair",
        ),
    )
}
