package corp.khin.solutions.booqi.domain.usecase

import corp.khin.solutions.booqi.domain.model.BlockedPeriod
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.DayOfWeek
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Escenario: "El Proveedor consulta su horario" (Grupo 3). */
class ObtenerHorarioUseCaseTest {

    private val repository = FakeAvailabilityRepository()
    private val obtener = ObtenerHorarioUseCase(repository)

    @Test
    fun `returns weekly hours Monday to Sunday and blocked periods chronologically`() = runTest {
        DefinirHorarioSemanalUseCase(repository)(
            "provider-1",
            listOf(workday(DayOfWeek.SUNDAY), workday(DayOfWeek.WEDNESDAY), workday(DayOfWeek.MONDAY)),
        )
        val bloquear = BloquearFechaHoraUseCase(repository)
        bloquear("provider-1", BlockedPeriod(FRIDAY_CHRISTMAS))
        bloquear("provider-1", BlockedPeriod(MONDAY_DATE, range(14, 15)))
        bloquear("provider-1", BlockedPeriod(MONDAY_DATE))
        bloquear("provider-1", BlockedPeriod(MONDAY_DATE, range(9, 10)))

        val availability = obtener("provider-1").value()

        assertEquals(
            listOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.SUNDAY),
            availability.weeklyHours.map { it.day },
        )
        assertEquals(
            listOf(
                BlockedPeriod(MONDAY_DATE),
                BlockedPeriod(MONDAY_DATE, range(9, 10)),
                BlockedPeriod(MONDAY_DATE, range(14, 15)),
                BlockedPeriod(FRIDAY_CHRISTMAS),
            ),
            availability.blockedPeriods,
        )
    }

    @Test
    fun `a provider without a schedule gets an empty availability not an error`() = runTest {
        val availability = obtener("provider-1").value()

        assertEquals("provider-1", availability.providerId)
        assertFalse(availability.isScheduleDefined)
        assertTrue(availability.blockedPeriods.isEmpty())
    }

    @Test
    fun `another provider's schedule is not visible`() = runTest {
        DefinirHorarioSemanalUseCase(repository)("provider-2", listOf(workday(DayOfWeek.MONDAY)))
        BloquearFechaHoraUseCase(repository)("provider-2", BlockedPeriod(FRIDAY_CHRISTMAS))

        val availability = obtener("provider-1").value()

        assertFalse(availability.isScheduleDefined)
        assertTrue(availability.blockedPeriods.isEmpty())
    }
}
