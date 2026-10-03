package corp.khin.solutions.booqi.data.mapper

import corp.khin.solutions.booqi.data.dto.ServiceDetailsDto
import corp.khin.solutions.booqi.data.dto.ServiceDto
import corp.khin.solutions.booqi.domain.model.Service
import corp.khin.solutions.booqi.domain.model.ServiceCategory
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
    category = category.toDomainCategory(),
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
    category = category.toDtoCategory(),
)

fun ServiceDetails.toDto(): ServiceDetailsDto = ServiceDetailsDto(
    title = title,
    photoUrl = photoUrl,
    description = description,
    priceCents = priceCents,
    durationMinutes = durationMinutes,
    modality = modality.toDtoModality(),
    category = category?.toDtoCategory(),
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

// Unlike modality, an unknown category isn't an error: it degrades to OTRO so a category added by a
// newer backend/app version doesn't make older clients fail to load the whole catalog.
private fun String.toDomainCategory(): ServiceCategory = when (this) {
    "barberia" -> ServiceCategory.BARBERIA
    "unas" -> ServiceCategory.UNAS
    "limpieza" -> ServiceCategory.LIMPIEZA
    "masajes" -> ServiceCategory.MASAJES
    "tecnico" -> ServiceCategory.TECNICO
    else -> ServiceCategory.OTRO
}

private fun ServiceCategory.toDtoCategory(): String = when (this) {
    ServiceCategory.BARBERIA -> "barberia"
    ServiceCategory.UNAS -> "unas"
    ServiceCategory.LIMPIEZA -> "limpieza"
    ServiceCategory.MASAJES -> "masajes"
    ServiceCategory.TECNICO -> "tecnico"
    ServiceCategory.OTRO -> "otro"
}
