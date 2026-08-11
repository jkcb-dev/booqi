package corp.khin.solutions.booqi.core.common

/**
 * Result of any operation that crosses a domain boundary (use case -> repository -> datasource).
 *
 * Every layer catches its own real exceptions and translates them into a [DomainError] at the
 * boundary it owns — a raw [Throwable] must never cross from `data` into `domain`, and must never
 * cross from `domain` into presentation. This keeps ViewModels doing exhaustive `when` over a
 * closed, known set of failures instead of guessing what a caught exception might mean.
 */
sealed interface DomainResult<out T> {
    data class Success<T>(val value: T) : DomainResult<T>
    data class Failure(val error: DomainError) : DomainResult<Nothing>
}

inline fun <T, R> DomainResult<T>.map(transform: (T) -> R): DomainResult<R> = when (this) {
    is DomainResult.Success -> DomainResult.Success(transform(value))
    is DomainResult.Failure -> this
}

inline fun <T> DomainResult<T>.onSuccess(action: (T) -> Unit): DomainResult<T> {
    if (this is DomainResult.Success) action(value)
    return this
}

inline fun <T> DomainResult<T>.onFailure(action: (DomainError) -> Unit): DomainResult<T> {
    if (this is DomainResult.Failure) action(error)
    return this
}

fun <T> T.asSuccess(): DomainResult<T> = DomainResult.Success(this)

fun DomainError.asFailure(): DomainResult<Nothing> = DomainResult.Failure(this)
