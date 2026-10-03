package corp.khin.solutions.booqi.feature.provider

import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.Month
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** Time-of-day text <-> `LocalTime`, and the Spanish names the schedule screens show. */
class ScheduleFormattingTest {

    @Test
    fun `time accepts HH mm H mm and the same digits without a colon`() {
        assertEquals(LocalTime(9, 0), parseTimeInput("09:00"))
        assertEquals(LocalTime(9, 30), parseTimeInput("9:30"))
        assertEquals(LocalTime(9, 30), parseTimeInput("0930"))
        assertEquals(LocalTime(9, 30), parseTimeInput("930"))
        assertEquals(LocalTime(18, 45), parseTimeInput("  18:45 "))
        assertEquals(LocalTime(0, 0), parseTimeInput("00:00"))
        assertEquals(LocalTime(23, 59), parseTimeInput("23:59"))
    }

    @Test
    fun `time rejects blank non numeric out of range and malformed input`() {
        listOf("", "  ", "abc", "9", "09", "24:00", "12:60", "12:5", "1:2:3", "-1:30", "12.30", "9am", "123456")
            .forEach { assertNull(parseTimeInput(it), "expected '$it' to be rejected") }
    }

    @Test
    fun `formatting a time round trips through parsing`() {
        assertEquals("09:05", formatTime(LocalTime(9, 5)))
        assertEquals("00:00", formatTime(LocalTime(0, 0)))
        assertEquals("18:30", formatTime(LocalTime(18, 30)))
        listOf(LocalTime(0, 0), LocalTime(9, 5), LocalTime(12, 0), LocalTime(23, 59)).forEach { time ->
            assertEquals(time, parseTimeInput(formatTime(time)))
        }
    }

    @Test
    fun `week starts on Monday and day names are Spanish`() {
        assertEquals(
            listOf("Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo"),
            DayOfWeek.entries.map { it.spanishName() },
        )
        assertEquals("LMXJVSD", DayOfWeek.entries.joinToString("") { it.spanishInitial() })
    }

    @Test
    fun `dates read as day of month year`() {
        assertEquals("25 de diciembre de 2026", LocalDate(2026, 12, 25).spanishText())
        assertEquals("Octubre", Month.OCTOBER.spanishName())
    }
}
