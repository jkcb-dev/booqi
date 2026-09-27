package corp.khin.solutions.booqi.domain.repository

import corp.khin.solutions.booqi.core.common.DomainResult
import corp.khin.solutions.booqi.domain.model.DateRange
import corp.khin.solutions.booqi.domain.model.ProviderProfile

/**
 * Domain-owned contract for the `ProviderProfile` aggregate (docs/domain/provider-flow.md
 * § Grupo 1 — Gestión de Perfil de Proveedor). The implementation (in `data`) decides how a
 * profile is sourced/persisted — this interface is the only thing a use case is allowed to know
 * about.
 */
interface ProviderProfileRepository {

    /**
     * Escenario: "Un usuario activa el modo Proveedor". Creates an empty [ProviderProfile] for
     * [userId]. Idempotent: if [userId] already has a profile, returns the existing one instead
     * of creating a duplicate — activation is a one-time transition per account.
     */
    suspend fun activateProviderMode(userId: String): DomainResult<ProviderProfile>

    /**
     * Escenario: "El Proveedor completa su perfil". Fills in the descriptive fields of the
     * profile identified by [profileId] and marks it complete. Callers must have already
     * validated required fields (see [corp.khin.solutions.booqi.domain.usecase.
     * CompletarPerfilDeProveedorUseCase]) — this method assumes valid input and focuses on
     * persistence.
     */
    suspend fun completeProfile(
        profileId: String,
        name: String,
        photoUrl: String,
        description: String,
        location: String,
    ): DomainResult<ProviderProfile>

    /**
     * Escenarios: "El Proveedor pausa su perfil por un rango de fechas" / "...reactiva su perfil
     * antes de tiempo". Sets the profile's vacation-mode date range, or clears it (reactivation)
     * when [pausedRange] is `null`.
     */
    suspend fun setPausedRange(profileId: String, pausedRange: DateRange?): DomainResult<ProviderProfile>
}
