package corp.khin.solutions.booqi.feature.booking

import corp.khin.solutions.booqi.domain.model.BookingStatus
import corp.khin.solutions.booqi.domain.model.CustomerReasonCode
import corp.khin.solutions.booqi.domain.model.ProviderReasonCode
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class BookingFormattingTest {

    @Test
    fun `a countdown reads days and hours then hours and minutes then minutes`() {
        assertEquals("2 d 3 h", countdownText(2.days + 3.hours + 40.minutes))
        assertEquals("23 h 0 min", countdownText(23.hours))
        assertEquals("3 h 20 min", countdownText(3.hours + 20.minutes))
        assertEquals("15 min", countdownText(15.minutes))
    }

    @Test
    fun `a countdown under a minute reads one minute`() {
        assertEquals("1 min", countdownText(20.seconds))
    }

    @Test
    fun `dates times and prices read in Spanish`() {
        assertEquals("sábado 3 de octubre", LocalDate(2026, 10, 3).spanishLongText())
        assertEquals("3 de octubre de 2026", LocalDate(2026, 10, 3).spanishText())
        assertEquals("09:05", formatTime(LocalTime(9, 5)))
        assertEquals("\$35.00", formatPrice(3500))
        assertEquals("\$12.05", formatPrice(1205))
        assertEquals("60 min", formatDuration(60))
    }

    @Test
    fun `every status and customer reason has wording`() {
        BookingStatus.entries.forEach { assertNotNull(it.customerLabel()) }
        assertEquals("Pendiente", BookingStatus.REQUESTED.customerLabel())
        assertEquals(CustomerReasonCode.entries.size, customerReasonOptions.size)
        assertEquals(CustomerReasonCode.OTHER, customerReasonFor("OTHER"))
        assertEquals(listOf("OTHER"), customerReasonOptions.filter { it.allowsNote }.map { it.id })
        ProviderReasonCode.entries.forEach { assertNotNull(it.label()) }
    }
}
