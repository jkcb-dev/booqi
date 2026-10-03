package corp.khin.solutions.booqi.data.dto

/**
 * Storage shape of one `provider_weekly_hours` row (docs/DATABASE.md). Deliberately not
 * `@Serializable` yet — same reasoning as [ProviderProfileDto]: no real backend contract exists to
 * shape it against (Supabase wiring lands with #27).
 *
 * [dayOfWeek] is the ISO day number (1 = Monday .. 7 = Sunday) and the times are ISO-8601 local
 * times (`"09:00"`), mirroring the table's `int`/`time` columns without presupposing the domain
 * layer's `kotlinx.datetime` types — the mapper translates.
 */
data class WeeklyHoursDto(
    val dayOfWeek: Int,
    val isActive: Boolean,
    val startTime: String,
    val endTime: String,
)

/**
 * Storage shape of one `provider_blocked_dates` row (docs/DATABASE.md). [blockedDate] is an ISO
 * date (`"2026-12-25"`); [startTime]/[endTime] are both `null` for a whole-day block, both set
 * (ISO local times) for a time range within the day.
 */
data class BlockedDateDto(
    val blockedDate: String,
    val startTime: String?,
    val endTime: String?,
)
