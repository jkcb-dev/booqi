package corp.khin.solutions.booqi.data.datasource

import corp.khin.solutions.booqi.data.dto.BlockedDateDto
import corp.khin.solutions.booqi.data.dto.WeeklyHoursDto

/**
 * TEMPORARY. In-memory stand-in for a real Supabase-backed [AvailabilityRemoteDataSource] until
 * #27 (Supabase schema + `supabase-kt` wiring) lands — see `docs/DATABASE.md`. Mirrors the pattern
 * in [FakeServiceRemoteDataSource]: this exists purely so the module graph and MVI wiring can be
 * proven end-to-end without blocking on backend implementation work. Replace, don't extend.
 *
 * Not thread-safe by design — a single fake, single-process instance has no concurrent-writer
 * scenario worth guarding against; a real datasource will get that from the backend instead.
 */
class FakeAvailabilityRemoteDataSource : AvailabilityRemoteDataSource {

    private val weeklyHoursByProvider = mutableMapOf<String, List<WeeklyHoursDto>>()
    private val blockedByProvider = mutableMapOf<String, List<BlockedDateDto>>()

    override suspend fun findWeeklyHours(providerId: String): List<WeeklyHoursDto> =
        weeklyHoursByProvider[providerId].orEmpty()

    override suspend fun replaceWeeklyHours(providerId: String, hours: List<WeeklyHoursDto>) {
        weeklyHoursByProvider[providerId] = hours
    }

    override suspend fun findBlockedDates(providerId: String): List<BlockedDateDto> =
        blockedByProvider[providerId].orEmpty()

    override suspend fun addBlockedDate(providerId: String, blocked: BlockedDateDto) {
        val current = blockedByProvider[providerId].orEmpty()
        if (blocked !in current) blockedByProvider[providerId] = current + blocked
    }

    override suspend fun removeBlockedDate(providerId: String, blocked: BlockedDateDto) {
        blockedByProvider[providerId] = blockedByProvider[providerId].orEmpty() - blocked
    }
}
