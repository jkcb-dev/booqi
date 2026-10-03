package corp.khin.solutions.booqi.feature.provider

import corp.khin.solutions.booqi.core.common.DomainError
import corp.khin.solutions.booqi.domain.model.BlockedPeriod
import kotlinx.datetime.LocalDate

/** The open "bloquear horas" form of P7: a time range within [date]. [error] is shown under the
 * two time fields (unparseable text, or the use case's "end after start" rule). */
data class BlockRangeFormState(
    val date: LocalDate,
    val startInput: String = "",
    val endInput: String = "",
    val error: String? = null,
)

/**
 * Immutable state of the date-blocking calendar (docs/design/SCREENS.md P7). [today] comes from
 * the injected `Clock`; [visibleMonth] is the first day of the month on screen. [blockedPeriods]
 * is the domain's chronological list as last returned.
 */
data class DateBlockingUiState(
    val today: LocalDate,
    val visibleMonth: LocalDate,
    /** Starts true so the first frame before the entry `Start` never flashes an empty calendar. */
    val isLoading: Boolean = true,
    val blockedPeriods: List<BlockedPeriod> = emptyList(),
    /** A block/unblock is in flight: further taps are ignored until it returns. */
    val isUpdating: Boolean = false,
    /** The schedule could not be loaded: the calendar body is replaced by a retry. */
    val loadError: DomainError? = null,
    /** A block/unblock that failed for a non-form reason (no connection...). */
    val actionError: String? = null,
    /** Non-null while the "bloquear horas" form is open. */
    val rangeForm: BlockRangeFormState? = null,
)
