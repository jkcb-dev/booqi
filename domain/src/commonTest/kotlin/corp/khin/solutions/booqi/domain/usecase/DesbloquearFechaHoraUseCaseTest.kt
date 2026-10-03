package corp.khin.solutions.booqi.domain.usecase

import corp.khin.solutions.booqi.domain.model.BlockedPeriod
import corp.khin.solutions.booqi.domain.model.DateRange
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.DayOfWeek
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Escenario: "El Proveedor desbloquea una fecha" (Grupo 3). */
class DesbloquearFechaHoraUseCaseTest {

    private val repository = FakeAvailabilityRepository()
    private val bloquear = BloquearFechaHoraUseCase(repository)
    private val desbloquear = DesbloquearFechaHoraUseCase(repository)

    private suspend fun slotCount(): Int {
        val availability = repository.getAvailability("provider-1").value()
        return GenerarTimeSlotsUseCase()(availability, 60, DateRange(FRIDAY_CHRISTMAS, FRIDAY_CHRISTMAS)).value().size
    }

    private suspend fun givenFridaySchedule() {
        DefinirHorarioSemanalUseCase(repository)("provider-1", listOf(workday(DayOfWeek.FRIDAY, 9, 12)))
    }

    @Test
    fun `unblocking a whole day makes it available again`() = runTest {
        givenFridaySchedule()
        bloquear("provider-1", BlockedPeriod(FRIDAY_CHRISTMAS))
        assertEquals(0, slotCount())

        val result = desbloquear("provider-1", BlockedPeriod(FRIDAY_CHRISTMAS))

        assertTrue(result.value().blockedPeriods.isEmpty())
        assertEquals(3, slotCount())
    }

    @Test
    fun `unblocking a time range leaves other blocks of that day`() = runTest {
        givenFridaySchedule()
        bloquear("provider-1", BlockedPeriod(FRIDAY_CHRISTMAS, range(9, 10)))
        bloquear("provider-1", BlockedPeriod(FRIDAY_CHRISTMAS, range(11, 12)))

        val result = desbloquear("provider-1", BlockedPeriod(FRIDAY_CHRISTMAS, range(9, 10)))

        assertEquals(listOf(BlockedPeriod(FRIDAY_CHRISTMAS, range(11, 12))), result.value().blockedPeriods)
    }

    @Test
    fun `unblocking something that is not blocked succeeds without changes`() = runTest {
        bloquear("provider-1", BlockedPeriod(FRIDAY_CHRISTMAS))

        val result = desbloquear("provider-1", BlockedPeriod(MONDAY_DATE))

        assertEquals(listOf(BlockedPeriod(FRIDAY_CHRISTMAS)), result.value().blockedPeriods)
    }
}
