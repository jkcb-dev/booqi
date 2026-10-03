package corp.khin.solutions.booqi.domain.usecase

import corp.khin.solutions.booqi.core.common.DomainError
import corp.khin.solutions.booqi.core.common.DomainResult
import corp.khin.solutions.booqi.domain.model.Availability
import corp.khin.solutions.booqi.domain.model.BlockedPeriod
import corp.khin.solutions.booqi.domain.model.DateRange
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Escenarios: "El Proveedor bloquea un día específico", "...bloquea solo un rango de horas de un
 * día" and "...bloquea un rango de horas inválido" (Grupo 3). "Las citas ya aceptadas ese día no
 * se ven afectadas" is structural — no `Booking` dependency — see the use case KDoc.
 */
class BloquearFechaHoraUseCaseTest {

    private val repository = FakeAvailabilityRepository()
    private val bloquear = BloquearFechaHoraUseCase(repository)
    private val generar = GenerarTimeSlotsUseCase()

    // Fridays 09:00-12:00, so 2026-12-25 (a Friday) would normally have slots.
    private suspend fun givenActiveSchedule(): Availability =
        DefinirHorarioSemanalUseCase(repository)("provider-1", listOf(workday(DayOfWeek.FRIDAY, 9, 12))).value()

    private suspend fun slotsOn(date: LocalDate) =
        generar(repository.getAvailability("provider-1").value(), 60, DateRange(date, date)).value()

    @Test
    fun `blocking a whole day removes it from new bookings`() = runTest {
        givenActiveSchedule()
        assertEquals(3, slotsOn(FRIDAY_CHRISTMAS).size)

        val result = bloquear("provider-1", BlockedPeriod(FRIDAY_CHRISTMAS))

        assertIs<DomainResult.Success<Availability>>(result)
        assertEquals(listOf(BlockedPeriod(FRIDAY_CHRISTMAS)), result.value.blockedPeriods)
        assertTrue(slotsOn(FRIDAY_CHRISTMAS).isEmpty())
    }

    @Test
    fun `blocking a time range removes only the overlapping slots`() = runTest {
        givenActiveSchedule()

        bloquear("provider-1", BlockedPeriod(FRIDAY_CHRISTMAS, range(10, 11))).value()

        assertEquals(listOf(time(9), time(11)), slotsOn(FRIDAY_CHRISTMAS).map { it.start })
    }

    @Test
    fun `a blocked range ending before it starts is rejected and nothing is blocked`() = runTest {
        givenActiveSchedule()
        val writesBefore = repository.writeCount

        val result = bloquear("provider-1", BlockedPeriod(FRIDAY_CHRISTMAS, range(14, 12)))

        assertIs<DomainResult.Failure>(result)
        assertIs<DomainError.InvalidInput>(result.error)
        assertEquals(writesBefore, repository.writeCount)
    }

    @Test
    fun `a blocked range with equal start and end is rejected`() = runTest {
        val result = bloquear("provider-1", BlockedPeriod(FRIDAY_CHRISTMAS, range(12, 12)))

        assertIs<DomainResult.Failure>(result)
        assertIs<DomainError.InvalidInput>(result.error)
    }

    @Test
    fun `blocking the same period twice is idempotent`() = runTest {
        bloquear("provider-1", BlockedPeriod(FRIDAY_CHRISTMAS))

        val result = bloquear("provider-1", BlockedPeriod(FRIDAY_CHRISTMAS))

        assertEquals(1, result.value().blockedPeriods.size)
    }
}
