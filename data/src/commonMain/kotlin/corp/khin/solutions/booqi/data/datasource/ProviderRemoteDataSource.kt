package corp.khin.solutions.booqi.data.datasource

import corp.khin.solutions.booqi.data.dto.ProviderDto

/**
 * Remote source of provider data. `data`-only concern — the shape it returns ([ProviderDto])
 * never crosses into `domain`; [corp.khin.solutions.booqi.data.mapper] does that translation.
 */
interface ProviderRemoteDataSource {
    suspend fun fetchFeaturedProviders(): List<ProviderDto>
}
