package corp.khin.solutions.booqi.data.mapper

import corp.khin.solutions.booqi.data.dto.ProviderDto
import corp.khin.solutions.booqi.domain.model.ServiceCategory
import corp.khin.solutions.booqi.domain.model.ServiceProvider

fun ProviderDto.toDomain(): ServiceProvider = ServiceProvider(
    id = id,
    name = name,
    category = runCatching { ServiceCategory.valueOf(category) }.getOrDefault(ServiceCategory.OTHER),
    ratingOutOf5 = ratingOutOf5,
    priceFromCents = priceFromCents,
    shortTagline = shortTagline,
)
