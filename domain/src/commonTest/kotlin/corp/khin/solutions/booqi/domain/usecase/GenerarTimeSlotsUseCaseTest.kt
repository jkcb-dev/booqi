package corp.khin.solutions.booqi.domain.usecase

import corp.khin.solutions.booqi.core.common.DomainError
import corp.khin.solutions.booqi.core.common.DomainResult
import corp.khin.solutions.booqi.domain.model.Availability
import corp.khin.solutions.booqi.domain.model.BlockedPeriod
import corp.khin.solutions.booqi.domain.model.DateRange
import corp.khin.solutions.booqi.domain.model.DayHours
import corp.khin.solutions.booqi.domain.model.TimeRange
import corp.khin.solutions.booqi.domain.model.TimeSlot
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Escenarios: "Se generan los TimeSlots según el horario y la duración del Servicio", "Los días
 * inactivos y los días sin horario no generan TimeSlots" and "Un perfil pausado no genera
 * TimeSlots durante la pausa" (Grupo 3), plus the edge cases of the algorithm. Pure function, so
 * no coroutines and no repository.
 */
class GenerarTimeSlotsUseCaseTest {

    private val generar = GenerarTimeSlotsUseCase()
    private val nextMonday = MONDAY_DATE.plus(7, DateTimeUnit.DAY)

    private fun availability(
        weekly: List<DayHours>,
        blocked: List<BlockedPeriod> = emptyList(),
    ) = Availability("provider-1", weekly, blocked)

    private fun mondayOnly(startHour: Int = 9, endHour: Int = 12, blocked: List<BlockedPeriod> = emptyList()) =
        availability(listOf(workday(DayOfWeek.MONDAY, startHour, endHour)), blocked)

    private fun generate(
        availability: Availability,
        duration: Int = 60,
        from: LocalDate = MONDAY_DATE,
        to: LocalDate = from,
        paused: DateRange? = null,
    ): List<TimeSlot> = generar(availability, duration, DateRange(from, to), paused).value()

    private fun List<TimeSlot>.starts() = map { it.start }

    // --- Escenario: Se generan los TimeSlots según el horario y la duración del Servicio ---

    @Test
    fun `slots are back to back from the start of the day`() {
        val slots = generate(mondayOnly(9, 12))

        assertEquals(
            listOf(
                TimeSlot("provider-1", MONDAY_DATE, time(9)),
                TimeSlot("provider-1", MONDAY_DATE, time(10)),
                TimeSlot("provider-1", MONDAY_DATE, time(11)),
            ),
            slots,
        )
    }

    @Test
    fun `a leftover shorter than the duration yields no slot`() {
        // 09:00-12:00 with 50 minutes: 09:00, 09:50, 10:40 (ends 11:30); 11:30 + 50 > 12:00.
        val slots = generate(mondayOnly(9, 12), duration = 50)

        assertEquals(listOf(time(9), time(9, 50), time(10, 40)), slots.starts())
    }

    @Test
    fun `a slot ending exactly at the end of the day is included`() {
        val slots = generate(mondayOnly(9, 12), duration = 45)

        assertEquals(listOf(time(9), time(9, 45), time(10, 30), time(11, 15)), slots.starts())
    }

    @Test
    fun `a duration equal to the whole window gives one slot`() {
        assertEquals(listOf(time(9)), generate(mondayOnly(9, 12), duration = 180).starts())
    }

    @Test
    fun `a duration longer than the window gives no slots`() {
        assertTrue(generate(mondayOnly(9, 12), duration = 181).isEmpty())
    }

    @Test
    fun `slots can start at midnight and end at the last minute of the day`() {
        val late = availability(
            listOf(DayHours(DayOfWeek.MONDAY, true, TimeRange(time(0), time(23, 59)))),
        )

        val slots = generate(late, duration = 720)

        // 00:00-12:00 fits; 12:00-24:00 does not (the day ends at 23:59).
        assertEquals(listOf(time(0)), slots.starts())
    }

    @Test
    fun `slots are ordered by date then start time across several days`() {
        val twoDays = availability(listOf(workday(DayOfWeek.TUESDAY, 9, 11), workday(DayOfWeek.MONDAY, 9, 11)))

        val slots = generate(twoDays, from = MONDAY_DATE, to = MONDAY_DATE.plus(8, DateTimeUnit.DAY))

        assertEquals(
            listOf(
                MONDAY_DATE to time(9),
                MONDAY_DATE to time(10),
                MONDAY_DATE.plus(1, DateTimeUnit.DAY) to time(9),
                MONDAY_DATE.plus(1, DateTimeUnit.DAY) to time(10),
                nextMonday to time(9),
                nextMonday to time(10),
                nextMonday.plus(1, DateTimeUnit.DAY) to time(9),
                nextMonday.plus(1, DateTimeUnit.DAY) to time(10),
            ),
            slots.map { it.date to it.start },
        )
    }

    @Test
    fun `both ends of the requested range are included`() {
        val slots = generate(mondayOnly(9, 10), from = MONDAY_DATE, to = nextMonday)

        assertEquals(listOf(MONDAY_DATE, nextMonday), slots.map { it.date })
    }

    // --- Escenario: Los días inactivos y los días sin horario no generan TimeSlots ---

    @Test
    fun `inactive days and days without a schedule produce nothing`() {
        val schedule = availability(
            listOf(
                workday(DayOfWeek.MONDAY, 9, 10),
                workday(DayOfWeek.WEDNESDAY, 9, 10, active = false),
            ),
        )

        val slots = generate(schedule, from = MONDAY_DATE, to = MONDAY_DATE.plus(6, DateTimeUnit.DAY))

        assertEquals(listOf(MONDAY_DATE), slots.map { it.date })
    }

    @Test
    fun `an empty availability produces nothing`() {
        assertTrue(generate(Availability("provider-1"), to = nextMonday).isEmpty())
    }

    // --- Blocked dates and times (Escenarios: bloquea un día / un rango de horas) ---

    @Test
    fun `a whole day block removes only that date`() {
        val schedule = mondayOnly(9, 10, blocked = listOf(BlockedPeriod(MONDAY_DATE)))

        val slots = generate(schedule, from = MONDAY_DATE, to = nextMonday)

        assertEquals(listOf(nextMonday), slots.map { it.date })
    }

    @Test
    fun `a blocked range removes every overlapping slot`() {
        // 09:30-10:30 touches both the 09:00 and the 10:00 slot.
        val schedule = mondayOnly(9, 12, blocked = listOf(BlockedPeriod(MONDAY_DATE, TimeRange(time(9, 30), time(10, 30)))))

        assertEquals(listOf(time(11)), generate(schedule).starts())
    }

    @Test
    fun `a block that only touches a slot edge does not remove it`() {
        // Block 10:00-11:00: the 09:00 slot ends at 10:00 and the 11:00 slot starts at 11:00.
        val schedule = mondayOnly(9, 12, blocked = listOf(BlockedPeriod(MONDAY_DATE, range(10, 11))))

        assertEquals(listOf(time(9), time(11)), generate(schedule).starts())
    }

    @Test
    fun `back to back blocks remove both of their slots`() {
        val schedule = mondayOnly(
            9,
            12,
            blocked = listOf(BlockedPeriod(MONDAY_DATE, range(9, 10)), BlockedPeriod(MONDAY_DATE, range(10, 11))),
        )

        assertEquals(listOf(time(11)), generate(schedule).starts())
    }

    @Test
    fun `slots are not re-aligned around a block`() {
        // 09:00-12:00 with 90 minutes: grid 09:00 and 10:30. Blocking 09:00-09:30 removes only 09:00.
        val schedule = mondayOnly(9, 12, blocked = listOf(BlockedPeriod(MONDAY_DATE, TimeRange(time(9), time(9, 30)))))

        assertEquals(listOf(time(10, 30)), generate(schedule, duration = 90).starts())
    }

    @Test
    fun `a blocked range outside the working hours changes nothing`() {
        val schedule = mondayOnly(9, 12, blocked = listOf(BlockedPeriod(MONDAY_DATE, range(18, 19))))

        assertEquals(listOf(time(9), time(10), time(11)), generate(schedule).starts())
    }

    @Test
    fun `a block on another date does not affect this date`() {
        val schedule = mondayOnly(9, 10, blocked = listOf(BlockedPeriod(nextMonday, range(9, 10))))

        assertEquals(listOf(time(9)), generate(schedule).starts())
    }

    // --- Escenario: Un perfil pausado no genera TimeSlots durante la pausa ---

    @Test
    fun `dates inside the paused range produce nothing both ends included`() {
        val thirdMonday = nextMonday.plus(7, DateTimeUnit.DAY)
        val schedule = mondayOnly(9, 10)

        // Paused from the first Monday through the second Monday, both inclusive.
        val slots = generate(schedule, from = MONDAY_DATE, to = thirdMonday, paused = DateRange(MONDAY_DATE, nextMonday))

        assertEquals(listOf(thirdMonday), slots.map { it.date })
    }

    @Test
    fun `the day before and the day after the pause generate normally`() {
        val schedule = availability(DayOfWeek.entries.map { workday(it, 9, 10) })
        val from = MONDAY_DATE
        val to = MONDAY_DATE.plus(4, DateTimeUnit.DAY)
        val pause = DateRange(MONDAY_DATE.plus(1, DateTimeUnit.DAY), MONDAY_DATE.plus(3, DateTimeUnit.DAY))

        val slots = generate(schedule, from = from, to = to, paused = pause)

        assertEquals(listOf(from, to), slots.map { it.date })
    }

    @Test
    fun `a pause outside the requested range changes nothing`() {
        val slots = generate(
            mondayOnly(9, 10),
            paused = DateRange(nextMonday, nextMonday.plus(10, DateTimeUnit.DAY)),
        )

        assertEquals(listOf(time(9)), slots.starts())
    }

    @Test
    fun `a single day pause blocks exactly that day`() {
        val slots = generate(mondayOnly(9, 10), to = nextMonday, paused = DateRange(MONDAY_DATE, MONDAY_DATE))

        assertEquals(listOf(nextMonday), slots.map { it.date })
    }

    // --- Validation ---

    @Test
    fun `a non positive duration is rejected`() {
        listOf(0, -30).forEach { duration ->
            val result = generar(mondayOnly(), duration, DateRange(MONDAY_DATE, MONDAY_DATE))

            assertIs<DomainResult.Failure>(result)
            assertIs<DomainError.InvalidInput>(result.error)
        }
    }

    @Test
    fun `slots carry the provider id of the availability`() {
        assertEquals("provider-1", generate(mondayOnly()).first().providerId)
    }
}
