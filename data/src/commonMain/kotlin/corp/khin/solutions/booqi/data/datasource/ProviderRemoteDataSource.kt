@file:Suppress("DEPRECATION") // the deprecated pre-correction cluster referencing itself; delete with #21

package corp.khin.solutions.booqi.data.datasource

import corp.khin.solutions.booqi.data.dto.ProviderDto

/**
 * Remote source of provider data. `data`-only concern — the shape it returns ([ProviderDto])
 * never crosses into `domain`; [corp.khin.solutions.booqi.data.mapper] does that translation.
 */
@Deprecated("Serves the pre-correction ServiceProvider model; replaced by CatalogRepositoryImpl. Remove with #21.")
interface ProviderRemoteDataSource {
    suspend fun fetchFeaturedProviders(): List<ProviderDto>
}
