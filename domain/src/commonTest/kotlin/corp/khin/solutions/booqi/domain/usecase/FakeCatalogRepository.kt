package corp.khin.solutions.booqi.domain.usecase

import corp.khin.solutions.booqi.core.common.DomainError
import corp.khin.solutions.booqi.core.common.DomainResult
import corp.khin.solutions.booqi.core.common.asFailure
import corp.khin.solutions.booqi.core.common.asSuccess
import corp.khin.solutions.booqi.domain.model.CatalogEntry
import corp.khin.solutions.booqi.domain.repository.CatalogRepository

/**
 * Hand-written fake for the Catalog search tests: returns [entries] as given (no visibility rules,
 * same contract as the real repository) and counts [calls] so a test can assert that an invalid
 * search never reached it.
 */
class FakeCatalogRepository(
    var entries: List<CatalogEntry> = emptyList(),
    var failure: DomainError? = null,
) : CatalogRepository {

    var calls = 0
        private set

    override suspend fun getEntries(): DomainResult<List<CatalogEntry>> {
        calls++
        return failure?.asFailure() ?: entries.asSuccess()
    }
}
