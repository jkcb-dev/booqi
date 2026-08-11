package corp.khin.solutions.booqi.core.common

/**
 * Closed set of failures that presentation code is allowed to know about. Data-layer
 * implementations (Ktor exceptions, SQLDelight exceptions, platform IO errors, ...) get mapped
 * into one of these at the repository boundary — that mapping is the repository's job, not the
 * ViewModel's.
 */
sealed interface DomainError {
    data object NoConnection : DomainError
    data object Timeout : DomainError
    data object NotFound : DomainError
    data object Unauthorized : DomainError
    data class Unknown(val message: String? = null) : DomainError
}
