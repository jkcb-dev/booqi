package corp.khin.solutions.booqi.feature.provider

import corp.khin.solutions.booqi.core.common.DomainResult
import corp.khin.solutions.booqi.domain.usecase.ObtenerUsuarioPublicoUseCase

/**
 * The names to show for the Customers [ids] (`Booking.customerId`), by id. A deleted account reads
 * "Usuario eliminado" (decided by the domain, `PublicUser`); an id whose lookup fails is left out,
 * and the screens then fall back to [CLIENT_LABEL] — a name is a nicety, never a reason to fail.
 */
internal suspend fun ObtenerUsuarioPublicoUseCase.namesOf(ids: Set<String>): Map<String, String> =
    ids.mapNotNull { id -> (this(id) as? DomainResult.Success)?.let { id to it.value.displayName } }.toMap()
