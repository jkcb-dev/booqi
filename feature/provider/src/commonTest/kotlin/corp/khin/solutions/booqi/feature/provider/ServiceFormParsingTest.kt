package corp.khin.solutions.booqi.feature.provider

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** Price/duration text -> domain integers (`priceCents`, `durationMinutes`) and back. */
class ServiceFormParsingTest {

    @Test
    fun `price accepts whole numbers and one or two decimals with dot or comma`() {
        assertEquals(1200, parsePriceCents("12"))
        assertEquals(1250, parsePriceCents("12.5"))
        assertEquals(1250, parsePriceCents("12,50"))
        assertEquals(5, parsePriceCents("0.05"))
        assertEquals(1250, parsePriceCents("  12.50 "))
    }

    @Test
    fun `price rejects blank non numeric zero negative and too precise input`() {
        listOf("", "  ", "abc", "12a", "0", "0.00", "-5", "1.234", "1.", ".5", "1e3", "99999999999")
            .forEach { assertNull(parsePriceCents(it), "expected '$it' to be rejected") }
    }

    @Test
    fun `duration accepts positive whole minutes only`() {
        assertEquals(45, parseDurationMinutes("45"))
        assertEquals(45, parseDurationMinutes(" 45 "))
        listOf("", "0", "-10", "4.5", "abc", "99999999999")
            .forEach { assertNull(parseDurationMinutes(it), "expected '$it' to be rejected") }
    }

    @Test
    fun `formatting a price round trips through parsing`() {
        listOf(1, 5, 100, 1250, 99_999).forEach { cents ->
            assertEquals(cents, parsePriceCents(formatPriceInput(cents)))
        }
        assertEquals("12.50", formatPriceInput(1250))
        assertEquals("0.05", formatPriceInput(5))
    }
}
