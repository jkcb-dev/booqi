package corp.khin.solutions.booqi.data.repository

import corp.khin.solutions.booqi.core.common.DomainError
import corp.khin.solutions.booqi.core.common.DomainResult
import corp.khin.solutions.booqi.core.common.asFailure

// Deliberate: this boundary is where every real exception gets translated into a DomainError — see
// core:common's DomainResult docs. Same pattern as the other repository implementations, factored
// out here because every method of these repositories does it.
@Suppress("TooGenericExceptionCaught")
internal inline fun <T> guarded(block: () -> DomainResult<T>): DomainResult<T> = try {
    block()
} catch (e: Exception) {
    DomainError.Unknown(e.message).asFailure()
}
