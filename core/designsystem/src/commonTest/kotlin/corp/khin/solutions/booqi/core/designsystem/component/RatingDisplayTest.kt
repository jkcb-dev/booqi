package corp.khin.solutions.booqi.core.designsystem.component

import kotlin.test.Test
import kotlin.test.assertEquals

class RatingDisplayTest {

    @Test
    fun `average is shown with one decimal`() {
        assertEquals("4.0", formatAverage(4.0))
        assertEquals("3.5", formatAverage(3.5))
        assertEquals("4.3", formatAverage(4.333))
    }

    @Test
    fun `average rounds half up and can carry into the units`() {
        assertEquals("4.7", formatAverage(4.666))
        assertEquals("5.0", formatAverage(4.96))
    }
}
