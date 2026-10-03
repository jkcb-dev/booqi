package corp.khin.solutions.booqi.domain.usecase

import corp.khin.solutions.booqi.core.common.DomainError
import corp.khin.solutions.booqi.core.common.DomainResult
import corp.khin.solutions.booqi.domain.model.Availability
import corp.khin.solutions.booqi.domain.model.DateRange
import corp.khin.solutions.booqi.domain.model.DayHours
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.DayOfWeek
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/** Escenarios: "El Proveedor define su horario semanal" and the validation rejection (Grupo 3). */
class DefinirHorarioSemanalUseCaseTest {

    private val repository = FakeAvailabilityRepository()
    private val useCase = DefinirHorarioSemanalUseCase(repository)

    @Test
    fun `defining the week stores it and customers can book within those hours`() = runTest {
        val week = DayOfWeek.entries.map { workday(it, 9, 12) }

        val result = useCase("provider-1", week)

        assertIs<DomainResult.Success<Availability>>(result)
        assertEquals(week, result.value.weeklyHours)
        assertTrue(result.value.isScheduleDefined)
        val slots = GenerarTimeSlotsUseCase()(
            result.value,
            durationMinutes = 60,
            range = DateRange(MONDAY_DATE, MONDAY_DATE),
        ).value()
        assertEquals(listOf(time(9), time(10), time(11)), slots.map { it.start })
    }

    @Test
    fun `defining only some days of the week is valid`() = runTest {
        val result = useCase("provider-1", listOf(workday(DayOfWeek.TUESDAY), workday(DayOfWeek.THURSDAY)))

        assertIs<DomainResult.Success<Availability>>(result)
        assertEquals(listOf(DayOfWeek.TUESDAY, DayOfWeek.THURSDAY), result.value.weeklyHours.map { it.day })
    }

    @Test
    fun `defining again replaces the previous schedule`() = runTest {
        useCase("provider-1", listOf(workday(DayOfWeek.MONDAY), workday(DayOfWeek.TUESDAY)))

        val result = useCase("provider-1", listOf(workday(DayOfWeek.FRIDAY, 10, 14)))

        assertEquals(listOf(workday(DayOfWeek.FRIDAY, 10, 14)), result.value().weeklyHours)
    }

    @Test
    fun `an active day ending before it starts is rejected and nothing is saved`() = runTest {
        val result = useCase("provider-1", listOf(workday(DayOfWeek.MONDAY, 17, 9)))

        assertIs<DomainResult.Failure>(result)
        assertIs<DomainError.InvalidInput>(result.error)
        assertEquals(0, repository.writeCount)
    }

    @Test
    fun `an active day with equal start and end is rejected`() = runTest {
        val result = useCase("provider-1", listOf(workday(DayOfWeek.MONDAY, 9, 9)))

        assertIs<DomainResult.Failure>(result)
        assertIs<DomainError.InvalidInput>(result.error)
    }

    @Test
    fun `the same day twice is rejected and nothing is saved`() = runTest {
        val result = useCase("provider-1", listOf(workday(DayOfWeek.MONDAY), workday(DayOfWeek.MONDAY, 10, 12)))

        assertIs<DomainResult.Failure>(result)
        assertIs<DomainError.InvalidInput>(result.error)
        assertEquals(0, repository.writeCount)
    }

    @Test
    fun `an inactive day keeps its range without validation`() = runTest {
        val inactive: DayHours = workday(DayOfWeek.SUNDAY, 18, 8, active = false)

        val result = useCase("provider-1", listOf(inactive))

        assertEquals(listOf(inactive), result.value().weeklyHours)
    }
}
