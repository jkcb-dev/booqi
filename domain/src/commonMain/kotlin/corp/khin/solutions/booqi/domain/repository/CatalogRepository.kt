package corp.khin.solutions.booqi.domain.repository

import corp.khin.solutions.booqi.core.common.DomainResult
import corp.khin.solutions.booqi.domain.model.CatalogEntry

/**
 * Domain-owned contract for the Catalog's read side (docs/domain/customer-flow.md § Grupo 1).
 *
 * The Catalog searches *Services*, so the one thing it needs from storage is every [CatalogEntry]:
 * a Service joined to its owning profile by `Service.providerId == ProviderProfile.id` (the
 * documented contract, docs/DOMAIN.md; #50 tracks the screens catching up). A Service whose profile
 * doesn't exist is **left out** — there is deliberately no fallback join by user id.
 *
 * The repository does *not* decide visibility: it returns active and disabled Services, complete
 * and incomplete profiles, paused or not. The rules (active, complete, not paused today, ...) live
 * in [corp.khin.solutions.booqi.domain.usecase.BuscarServiciosUseCase] so they are domain logic,
 * testable without I/O. A real backend may push them down as a query later without changing them.
 */
interface CatalogRepository {
    suspend fun getEntries(): DomainResult<List<CatalogEntry>>
}
