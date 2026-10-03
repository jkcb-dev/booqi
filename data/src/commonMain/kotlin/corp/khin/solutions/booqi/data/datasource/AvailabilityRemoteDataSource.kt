package corp.khin.solutions.booqi.data.datasource

import corp.khin.solutions.booqi.data.dto.BlockedDateDto
import corp.khin.solutions.booqi.data.dto.WeeklyHoursDto

/**
 * Source of a Provider's schedule data, one method group per backing table
 * (`provider_weekly_hours`, `provider_blocked_dates` — docs/DATABASE.md). `data`-only concern; the
 * DTO shapes never cross into `domain` ([corp.khin.solutions.booqi.data.mapper] translates).
 * Ordering is not part of this contract — the repository normalizes it.
 */
interface AvailabilityRemoteDataSource {

    /** The provider's weekly-hours rows; empty if none were ever saved. */
    suspend fun findWeeklyHours(providerId: String): List<WeeklyHoursDto>

    /** Replaces all of [providerId]'s weekly-hours rows with [hours]. */
    suspend fun replaceWeeklyHours(providerId: String, hours: List<WeeklyHoursDto>)

    /** The provider's blocked-date rows; empty if none. */
    suspend fun findBlockedDates(providerId: String): List<BlockedDateDto>

    /** Adds [blocked] unless an equal row already exists for [providerId]. */
    suspend fun addBlockedDate(providerId: String, blocked: BlockedDateDto)

    /** Removes the row equal to [blocked] for [providerId], if any. */
    suspend fun removeBlockedDate(providerId: String, blocked: BlockedDateDto)
}
