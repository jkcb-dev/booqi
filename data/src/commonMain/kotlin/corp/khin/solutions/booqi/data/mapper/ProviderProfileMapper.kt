package corp.khin.solutions.booqi.data.mapper

import corp.khin.solutions.booqi.data.dto.ProviderProfileDto
import corp.khin.solutions.booqi.domain.model.DateRange
import corp.khin.solutions.booqi.domain.model.ProviderProfile
import kotlinx.datetime.LocalDate

fun ProviderProfileDto.toDomain(): ProviderProfile = ProviderProfile(
    id = id,
    userId = userId,
    name = name,
    photoUrl = photoUrl,
    description = description,
    location = location,
    isComplete = isComplete,
    pausedRange = toDomainPausedRange(),
    ratingAverage = ratingAverage,
    ratingCount = ratingCount,
)

private fun ProviderProfileDto.toDomainPausedRange(): DateRange? {
    val start = pausedRangeStart
    val end = pausedRangeEnd
    return if (start != null && end != null) {
        DateRange(LocalDate.parse(start), LocalDate.parse(end))
    } else {
        null
    }
}

fun ProviderProfile.toDto(): ProviderProfileDto = ProviderProfileDto(
    id = id,
    userId = userId,
    name = name,
    photoUrl = photoUrl,
    description = description,
    location = location,
    isComplete = isComplete,
    pausedRangeStart = pausedRange?.start?.toString(),
    pausedRangeEnd = pausedRange?.end?.toString(),
    ratingAverage = ratingAverage,
    ratingCount = ratingCount,
)
