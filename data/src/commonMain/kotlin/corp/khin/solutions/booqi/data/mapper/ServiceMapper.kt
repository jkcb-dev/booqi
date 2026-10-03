package corp.khin.solutions.booqi.data.mapper

import corp.khin.solutions.booqi.data.dto.ServiceDetailsDto
import corp.khin.solutions.booqi.data.dto.ServiceDto
import corp.khin.solutions.booqi.domain.model.Service
import corp.khin.solutions.booqi.domain.model.ServiceDetails
import corp.khin.solutions.booqi.domain.model.ServiceModality

fun ServiceDto.toDomain(): Service = Service(
    id = id,
    providerId = providerId,
    title = title,
    photoUrl = photoUrl,
    description = description,
    priceCents = priceCents,
    durationMinutes = durationMinutes,
    modality = modality.toDomainModality(),
    isActive = isActive,
)

fun Service.toDto(): ServiceDto = ServiceDto(
    id = id,
    providerId = providerId,
    title = title,
    photoUrl = photoUrl,
    description = description,
    priceCents = priceCents,
    durationMinutes = durationMinutes,
    modality = modality.toDtoModality(),
    isActive = isActive,
)

fun ServiceDetails.toDto(): ServiceDetailsDto = ServiceDetailsDto(
    title = title,
    photoUrl = photoUrl,
    description = description,
    priceCents = priceCents,
    durationMinutes = durationMinutes,
    modality = modality.toDtoModality(),
)

private fun String.toDomainModality(): ServiceModality = when (this) {
    "local" -> ServiceModality.LOCAL
    "domicilio" -> ServiceModality.DOMICILIO
    "ambos" -> ServiceModality.AMBOS
    else -> error("Unknown Service modality: $this")
}

private fun ServiceModality.toDtoModality(): String = when (this) {
    ServiceModality.LOCAL -> "local"
    ServiceModality.DOMICILIO -> "domicilio"
    ServiceModality.AMBOS -> "ambos"
}
