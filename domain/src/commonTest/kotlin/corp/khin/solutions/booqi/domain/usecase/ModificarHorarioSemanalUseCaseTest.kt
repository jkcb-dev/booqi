package corp.khin.solutions.booqi.domain.usecase

import corp.khin.solutions.booqi.core.common.DomainError
import corp.khin.solutions.booqi.core.common.DomainResult
import corp.khin.solutions.booqi.domain.model.Availability
import corp.khin.solutions.booqi.domain.model.BlockedPeriod
import corp.khin.solutions.booqi.domain.model.DateRange
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.DayOfWeek
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

/**
 * Escenario: "El Proveedor modifica su horario semanal" plus the NotFound and validation edges
 * (Grupo 3). The "citas ya aceptadas NO se cancelan" clause is structural — no `Booking` exists
 * and this use case has no dependency on one (see its KDoc) — so it has nothing to assert here.
 */
class ModificarHorarioSemanalUseCaseTest {

    private val repository = FakeAvailabilityRepository()
    private val modificar = ModificarHorarioSemanalUseCase(repository)

    private suspend fun defineMonToWed() {
        DefinirHorarioSemanalUseCase(repository)(
            "provider-1",
            listOf(
                workday(DayOfWeek.MONDAY),
                workday(DayOfWeek.TUESDAY),
                workday(DayOfWeek.WEDNESDAY),
            ),
        )
    }

    @Test
    fun `changing one day keeps the others and applies to future slot generation`() = runTest {
        defineMonToWed()

        val result = modificar("provider-1", listOf(workday(DayOfWeek.MONDAY, 13, 15)))

        assertIs<DomainResult.Success<Availability>>(result)
        assertEquals(
            listOf(
                workday(DayOfWeek.MONDAY, 13, 15),
                workday(DayOfWeek.TUESDAY),
                workday(DayOfWeek.WEDNESDAY),
            ),
            result.value.weeklyHours,
        )
        val slots = GenerarTimeSlotsUseCase()(result.value, 60, DateRange(MONDAY_DATE, MONDAY_DATE)).value()
        assertEquals(listOf(time(13), time(14)), slots.map { it.start })
    }

    @Test
    fun `a day the provider never worked can be added`() = runTest {
        defineMonToWed()

        val result = modificar("provider-1", listOf(workday(DayOfWeek.SATURDAY, 10, 13)))

        assertEquals(
            listOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.SATURDAY),
            result.value().weeklyHours.map { it.day },
        )
    }

    @Test
    fun `a day can be switched off`() = runTest {
        defineMonToWed()

        val result = modificar("provider-1", listOf(workday(DayOfWeek.TUESDAY, active = false)))

        assertEquals(false, result.value().weeklyHours.first { it.day == DayOfWeek.TUESDAY }.isActive)
    }

    @Test
    fun `modifying without a defined schedule is NotFound and saves nothing`() = runTest {
        val result = modificar("provider-1", listOf(workday(DayOfWeek.MONDAY)))

        assertIs<DomainResult.Failure>(result)
        assertEquals(DomainError.NotFound, result.error)
        assertEquals(0, repository.writeCount)
    }

    @Test
    fun `an invalid change is rejected and the stored schedule is unchanged`() = runTest {
        defineMonToWed()
        val writesBefore = repository.writeCount

        val result = modificar("provider-1", listOf(workday(DayOfWeek.MONDAY, 17, 9)))

        assertIs<DomainResult.Failure>(result)
        assertIs<DomainError.InvalidInput>(result.error)
        assertEquals(writesBefore, repository.writeCount)
        assertEquals(workday(DayOfWeek.MONDAY), repository.getAvailability("provider-1").value().weeklyHours.first())
    }

    @Test
    fun `the same day twice in the changes is rejected`() = runTest {
        defineMonToWed()

        val result = modificar("provider-1", listOf(workday(DayOfWeek.MONDAY), workday(DayOfWeek.MONDAY, 10, 12)))

        assertIs<DomainResult.Failure>(result)
        assertIs<DomainError.InvalidInput>(result.error)
    }

    @Test
    fun `modifying leaves blocked periods untouched`() = runTest {
        defineMonToWed()
        BloquearFechaHoraUseCase(repository)("provider-1", BlockedPeriod(FRIDAY_CHRISTMAS))

        val result = modificar("provider-1", listOf(workday(DayOfWeek.MONDAY, 10, 12)))

        assertEquals(1, result.value().blockedPeriods.size)
    }
}
