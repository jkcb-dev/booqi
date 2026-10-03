@file:Suppress("DEPRECATION") // the deprecated pre-correction cluster referencing itself; delete with #21

package corp.khin.solutions.booqi.data.mapper

import corp.khin.solutions.booqi.data.dto.ProviderDto
import corp.khin.solutions.booqi.domain.model.LegacyServiceCategory
import corp.khin.solutions.booqi.domain.model.ServiceProvider

@Deprecated("Maps the pre-correction ServiceProvider model; remove with #21.")
fun ProviderDto.toDomain(): ServiceProvider = ServiceProvider(
    id = id,
    name = name,
    category = runCatching { LegacyServiceCategory.valueOf(category) }.getOrDefault(LegacyServiceCategory.OTHER),
    ratingOutOf5 = ratingOutOf5,
    priceFromCents = priceFromCents,
    shortTagline = shortTagline,
)
