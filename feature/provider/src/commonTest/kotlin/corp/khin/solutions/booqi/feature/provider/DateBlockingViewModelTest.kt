@file:OptIn(ExperimentalCoroutinesApi::class)

package corp.khin.solutions.booqi.feature.provider

import corp.khin.solutions.booqi.core.common.DomainError
import corp.khin.solutions.booqi.domain.model.BlockedPeriod
import corp.khin.solutions.booqi.domain.model.TimeRange
import corp.khin.solutions.booqi.domain.usecase.BloquearFechaHoraUseCase
import corp.khin.solutions.booqi.domain.usecase.DesbloquearFechaHoraUseCase
import corp.khin.solutions.booqi.domain.usecase.ObtenerHorarioUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** A [Clock] the test moves by hand. */
private class FakeClock(var instant: Instant) : Clock {
    override fun now(): Instant = instant
}

/**
 * Grupo 3 scenarios (docs/domain/provider-flow.md) that surface on the P7 blocking calendar,
 * exercised through [DateBlockingViewModel]'s reducer: state and events out, for actions in.
 */
class DateBlockingViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val repository = FakeAvailabilityRepository()

    // Saturday 3 October 2026, midday UTC.
    private val clock = FakeClock(Instant.parse("2026-10-03T12:00:00Z"))
    private val today = LocalDate(2026, 10, 3)

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /** Builds the ViewModel and sends the Start [ScheduleManagementScreen] sends on every entry. */
    private fun newViewModel(timeZone: TimeZone = TimeZone.UTC) = DateBlockingViewModel(
        obtenerHorario = ObtenerHorarioUseCase(repository),
        bloquearFechaHora = BloquearFechaHoraUseCase(repository),
        desbloquearFechaHora = DesbloquearFechaHoraUseCase(repository),
        clock = clock,
        timeZone = timeZone,
    ).also { it.onAction(DateBlockingAction.Start) }

    private fun range(startHour: Int, endHour: Int) = TimeRange(LocalTime(startHour, 0), LocalTime(endHour, 0))

    private suspend fun TestScope.awaitEventAfter(
        viewModel: DateBlockingViewModel,
        action: DateBlockingAction,
    ): DateBlockingEvent {
        var event: DateBlockingEvent? = null
        val collectorJob = launch { event = viewModel.events.first() }
        viewModel.onAction(action)
        advanceUntilIdle()
        collectorJob.join()
        return checkNotNull(event) { "No event received for $action" }
    }

    /** Opens the form and types [start]..[end] for [date], as the screen's controls do. */
    private fun DateBlockingViewModel.fillRangeForm(date: LocalDate, start: String, end: String) {
        onAction(DateBlockingAction.ShowRangeForm)
        onAction(DateBlockingAction.RangeDateChanged(date))
        onAction(DateBlockingAction.RangeStartChanged(start))
        onAction(DateBlockingAction.RangeEndChanged(end))
    }

    // --- Today and month navigation ---------------------------------------------------------------

    @Test
    fun `today and the visible month come from the injected clock`() = runTest(testDispatcher) {
        val viewModel = newViewModel()
        advanceUntilIdle()

        assertEquals(today, viewModel.state.value.today)
        assertEquals(LocalDate(2026, 10, 1), viewModel.state.value.visibleMonth)
    }

    @Test
    fun `today follows the clock time zone across midnight`() = runTest(testDispatcher) {
        clock.instant = Instant.parse("2026-12-31T23:30:00Z")

        val utc = newViewModel(TimeZone.UTC)
        val tokyo = newViewModel(TimeZone.of("Asia/Tokyo"))

        assertEquals(LocalDate(2026, 12, 31), utc.state.value.today)
        assertEquals(LocalDate(2027, 1, 1), tokyo.state.value.today)
        assertEquals(LocalDate(2027, 1, 1), tokyo.state.value.visibleMonth)
    }

    @Test
    fun `month navigation moves back and forth and rolls the year`() = runTest(testDispatcher) {
        val viewModel = newViewModel()
        advanceUntilIdle()

        viewModel.onAction(DateBlockingAction.NextMonth)
        viewModel.onAction(DateBlockingAction.NextMonth)
        viewModel.onAction(DateBlockingAction.NextMonth)
        assertEquals(LocalDate(2027, 1, 1), viewModel.state.value.visibleMonth)

        viewModel.onAction(DateBlockingAction.PreviousMonth)
        assertEquals(LocalDate(2026, 12, 1), viewModel.state.value.visibleMonth)
        repeat(12) { viewModel.onAction(DateBlockingAction.PreviousMonth) }
        assertEquals(LocalDate(2025, 12, 1), viewModel.state.value.visibleMonth)
        assertEquals(today, viewModel.state.value.today, "navigating never moves today")
    }

    // --- Escenario: "El Proveedor consulta su horario" --------------------------------------------

    @Test
    fun `a provider without blocked dates gets an empty calendar and no error`() = runTest(testDispatcher) {
        val viewModel = newViewModel()

        advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse(state.isLoading)
        assertTrue(state.blockedPeriods.isEmpty())
        assertNull(state.loadError)
        assertNull(state.rangeForm)
    }

    @Test
    fun `blocked dates load in chronological order`() = runTest(testDispatcher) {
        val dec25 = BlockedPeriod(LocalDate(2026, 12, 25))
        val oct10 = BlockedPeriod(LocalDate(2026, 10, 10), range(12, 14))
        val oct05 = BlockedPeriod(LocalDate(2026, 10, 5))
        repository.seedBlocked(listOf(dec25, oct10, oct05))
        val viewModel = newViewModel()

        advanceUntilIdle()

        assertEquals(listOf(oct05, oct10, dec25), viewModel.state.value.blockedPeriods)
    }

    @Test
    fun `the first frame before the entry Start is loading and not an empty calendar`() {
        val viewModel = DateBlockingViewModel(
            obtenerHorario = ObtenerHorarioUseCase(repository),
            bloquearFechaHora = BloquearFechaHoraUseCase(repository),
            desbloquearFechaHora = DesbloquearFechaHoraUseCase(repository),
            clock = clock,
            timeZone = TimeZone.UTC,
        )

        assertTrue(viewModel.state.value.isLoading)
    }

    // --- Escenario: "El Proveedor bloquea un día específico" --------------------------------------

    @Test
    fun `tapping a free date blocks the whole day`() = runTest(testDispatcher) {
        val viewModel = newViewModel()
        advanceUntilIdle()
        val dec25 = LocalDate(2026, 12, 25)

        viewModel.onAction(DateBlockingAction.DateTapped(dec25))
        advanceUntilIdle()

        assertEquals(listOf(BlockedPeriod(dec25)), viewModel.state.value.blockedPeriods)
        assertEquals(DateBlockState.FullyBlocked, viewModel.state.value.blockedPeriods.blockStateOf(dec25))
        assertEquals(listOf(BlockedPeriod(dec25)), repository.stored().blockedPeriods)
        assertFalse(viewModel.state.value.isUpdating)
        assertNull(viewModel.state.value.actionError)
    }

    @Test
    fun `a double tap on a date blocks it once and does not toggle it back`() = runTest(testDispatcher) {
        val viewModel = newViewModel()
        advanceUntilIdle()
        val dec25 = LocalDate(2026, 12, 25)

        viewModel.onAction(DateBlockingAction.DateTapped(dec25))
        viewModel.onAction(DateBlockingAction.DateTapped(dec25))
        assertTrue(viewModel.state.value.isUpdating)
        advanceUntilIdle()

        assertEquals(listOf(BlockedPeriod(dec25)), viewModel.state.value.blockedPeriods)
        assertEquals(1, repository.writeCount)
    }

    @Test
    fun `blocking leaves the other blocked dates alone and keeps them in order`() = runTest(testDispatcher) {
        repository.seedBlocked(listOf(BlockedPeriod(LocalDate(2026, 12, 25))))
        val viewModel = newViewModel()
        advanceUntilIdle()

        viewModel.onAction(DateBlockingAction.DateTapped(LocalDate(2026, 10, 20)))
        advanceUntilIdle()

        assertEquals(
            listOf(LocalDate(2026, 10, 20), LocalDate(2026, 12, 25)),
            viewModel.state.value.blockedPeriods.map { it.date },
        )
    }

    // --- Escenario: "El Proveedor desbloquea una fecha" -------------------------------------------

    @Test
    fun `tapping a blocked date unblocks it`() = runTest(testDispatcher) {
        val dec25 = LocalDate(2026, 12, 25)
        repository.seedBlocked(listOf(BlockedPeriod(dec25)))
        val viewModel = newViewModel()
        advanceUntilIdle()

        viewModel.onAction(DateBlockingAction.DateTapped(dec25))
        advanceUntilIdle()

        assertTrue(viewModel.state.value.blockedPeriods.isEmpty())
        assertTrue(repository.stored().blockedPeriods.isEmpty())
        assertEquals(DateBlockState.Free, viewModel.state.value.blockedPeriods.blockStateOf(dec25))
    }

    @Test
    fun `tapping a date that only has a blocked time range blocks the whole day on top of it`() =
        runTest(testDispatcher) {
            val day = LocalDate(2026, 10, 10)
            val part = BlockedPeriod(day, range(12, 14))
            repository.seedBlocked(listOf(part))
            val viewModel = newViewModel()
            advanceUntilIdle()
            assertEquals(DateBlockState.PartiallyBlocked, viewModel.state.value.blockedPeriods.blockStateOf(day))

            viewModel.onAction(DateBlockingAction.DateTapped(day))
            advanceUntilIdle()

            assertEquals(DateBlockState.FullyBlocked, viewModel.state.value.blockedPeriods.blockStateOf(day))
            assertTrue(part in viewModel.state.value.blockedPeriods, "the range block is not discarded")
        }

    @Test
    fun `unblocking from the list removes exactly that period`() = runTest(testDispatcher) {
        val day = LocalDate(2026, 10, 10)
        val morning = BlockedPeriod(day, range(9, 11))
        val afternoon = BlockedPeriod(day, range(14, 16))
        repository.seedBlocked(listOf(morning, afternoon, BlockedPeriod(day)))
        val viewModel = newViewModel()
        advanceUntilIdle()

        viewModel.onAction(DateBlockingAction.Unblock(morning))
        advanceUntilIdle()

        assertEquals(listOf(BlockedPeriod(day), afternoon), viewModel.state.value.blockedPeriods)
    }

    @Test
    fun `unblocking something that is not blocked is a harmless no-op`() = runTest(testDispatcher) {
        repository.seedBlocked(listOf(BlockedPeriod(LocalDate(2026, 12, 25))))
        val viewModel = newViewModel()
        advanceUntilIdle()
        val before = viewModel.state.value.blockedPeriods

        viewModel.onAction(DateBlockingAction.Unblock(BlockedPeriod(LocalDate(2026, 11, 1))))
        advanceUntilIdle()

        assertEquals(before, viewModel.state.value.blockedPeriods)
        assertNull(viewModel.state.value.actionError)
    }

    // --- Escenario: "El Proveedor bloquea solo un rango de horas de un día" -----------------------

    @Test
    fun `the range form opens on today and blocks only the typed hours`() = runTest(testDispatcher) {
        val viewModel = newViewModel()
        advanceUntilIdle()

        viewModel.onAction(DateBlockingAction.ShowRangeForm)
        assertEquals(today, viewModel.state.value.rangeForm?.date)

        val day = LocalDate(2026, 10, 14)
        viewModel.fillRangeForm(day, "12:00", "14:00")
        viewModel.onAction(DateBlockingAction.ConfirmRange)
        advanceUntilIdle()

        val period = BlockedPeriod(day, range(12, 14))
        assertEquals(listOf(period), viewModel.state.value.blockedPeriods)
        assertEquals(listOf(period), repository.stored().blockedPeriods)
        assertNull(viewModel.state.value.rangeForm, "a successful block closes the form")
        // Only the hours: the date is not painted as fully blocked.
        assertEquals(DateBlockState.PartiallyBlocked, viewModel.state.value.blockedPeriods.blockStateOf(day))
    }

    @Test
    fun `an unusual but valid time format is accepted in the range form`() = runTest(testDispatcher) {
        val viewModel = newViewModel()
        advanceUntilIdle()

        viewModel.fillRangeForm(today, "9:30", "1100")
        viewModel.onAction(DateBlockingAction.ConfirmRange)
        advanceUntilIdle()

        assertEquals(
            TimeRange(LocalTime(9, 30), LocalTime(11, 0)),
            viewModel.state.value.blockedPeriods.single().timeRange,
        )
    }

    // --- Escenario: "El Proveedor bloquea un rango de horas inválido" -----------------------------

    @Test
    fun `an end at or before the start is a form error and nothing is blocked`() = runTest(testDispatcher) {
        val viewModel = newViewModel()
        advanceUntilIdle()

        listOf("14:00" to "14:00", "14:00" to "12:00").forEach { (start, end) ->
            viewModel.fillRangeForm(today, start, end)
            viewModel.onAction(DateBlockingAction.ConfirmRange)
            advanceUntilIdle()

            val form = checkNotNull(viewModel.state.value.rangeForm) { "the form stays open" }
            assertEquals("La hora de fin debe ser posterior a la hora de inicio", form.error)
            assertTrue(viewModel.state.value.blockedPeriods.isEmpty())
            assertEquals(0, repository.writeCount)
            assertNull(viewModel.state.value.actionError)
            assertFalse(viewModel.state.value.isUpdating)
        }
    }

    @Test
    fun `text that is not a time is a form error and never reaches the use case`() = runTest(testDispatcher) {
        val viewModel = newViewModel()
        advanceUntilIdle()

        viewModel.fillRangeForm(today, "doce", "")
        viewModel.onAction(DateBlockingAction.ConfirmRange)
        advanceUntilIdle()

        assertEquals(INVALID_TIME_MESSAGE, viewModel.state.value.rangeForm?.error)
        assertEquals(0, repository.writeCount)
    }

    @Test
    fun `editing a form field clears the form error and a corrected range then blocks`() =
        runTest(testDispatcher) {
            val viewModel = newViewModel()
            advanceUntilIdle()
            viewModel.fillRangeForm(today, "14:00", "12:00")
            viewModel.onAction(DateBlockingAction.ConfirmRange)
            advanceUntilIdle()
            assertTrue(viewModel.state.value.rangeForm?.error != null)

            viewModel.onAction(DateBlockingAction.RangeEndChanged("15:00"))
            assertNull(viewModel.state.value.rangeForm?.error)
            viewModel.onAction(DateBlockingAction.ConfirmRange)
            advanceUntilIdle()

            assertEquals(range(14, 15), viewModel.state.value.blockedPeriods.single().timeRange)
            assertNull(viewModel.state.value.rangeForm)
        }

    @Test
    fun `dismissing the range form discards what was typed`() = runTest(testDispatcher) {
        val viewModel = newViewModel()
        advanceUntilIdle()
        viewModel.fillRangeForm(today, "12:00", "14:00")

        viewModel.onAction(DateBlockingAction.DismissRangeForm)
        viewModel.onAction(DateBlockingAction.ShowRangeForm)

        assertEquals(BlockRangeFormState(date = today), viewModel.state.value.rangeForm)
        assertEquals(0, repository.writeCount)
    }

    @Test
    fun `tapping a date while the range form is open keeps the form`() = runTest(testDispatcher) {
        val viewModel = newViewModel()
        advanceUntilIdle()
        viewModel.fillRangeForm(today, "12:00", "14:00")

        viewModel.onAction(DateBlockingAction.DateTapped(LocalDate(2026, 10, 20)))
        advanceUntilIdle()

        assertEquals("12:00", viewModel.state.value.rangeForm?.startInput)
        assertEquals(1, viewModel.state.value.blockedPeriods.size)
    }

    // --- Failures ---------------------------------------------------------------------------------

    @Test
    fun `a failing block shows the failure and leaves the calendar as it was`() = runTest(testDispatcher) {
        val viewModel = newViewModel()
        advanceUntilIdle()
        repository.writeFailure = DomainError.NoConnection

        val event = awaitEventAfter(viewModel, DateBlockingAction.DateTapped(LocalDate(2026, 12, 25)))

        assertEquals(DateBlockingEvent.ShowError("Sin conexión"), event)
        assertEquals("Sin conexión", viewModel.state.value.actionError)
        assertTrue(viewModel.state.value.blockedPeriods.isEmpty())
        assertFalse(viewModel.state.value.isUpdating)
    }

    @Test
    fun `a failing load exposes the error and a Start after it recovers`() = runTest(testDispatcher) {
        repository.seedBlocked(listOf(BlockedPeriod(LocalDate(2026, 12, 25))))
        repository.readFailure = DomainError.NoConnection
        val viewModel = newViewModel()
        advanceUntilIdle()
        assertEquals(DomainError.NoConnection, viewModel.state.value.loadError)
        assertFalse(viewModel.state.value.isLoading)

        repository.readFailure = null
        viewModel.onAction(DateBlockingAction.Start) // the "Reintentar" button
        advanceUntilIdle()

        assertNull(viewModel.state.value.loadError)
        assertEquals(1, viewModel.state.value.blockedPeriods.size)
    }

    @Test
    fun `taps are ignored until the calendar has loaded`() = runTest(testDispatcher) {
        repository.readFailure = DomainError.Timeout
        val viewModel = newViewModel()
        advanceUntilIdle()

        viewModel.onAction(DateBlockingAction.DateTapped(LocalDate(2026, 12, 25)))
        advanceUntilIdle()

        assertEquals(0, repository.writeCount)
    }

    // --- Re-entry (ViewModels are not destination-scoped) -----------------------------------------

    @Test
    fun `re-entering shows fresh data the current month and a closed form`() = runTest(testDispatcher) {
        val viewModel = newViewModel()
        advanceUntilIdle()
        viewModel.onAction(DateBlockingAction.NextMonth)
        viewModel.onAction(DateBlockingAction.NextMonth)
        viewModel.fillRangeForm(today, "12:00", "14:00")
        repository.seedBlocked(listOf(BlockedPeriod(LocalDate(2026, 12, 25)))) // changed elsewhere
        clock.instant = Instant.parse("2026-10-04T08:00:00Z") // the app stayed open past midnight

        viewModel.onAction(DateBlockingAction.Start) // what ScheduleManagementScreen sends on entry
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(LocalDate(2026, 10, 1), state.visibleMonth)
        assertEquals(LocalDate(2026, 10, 4), state.today)
        assertNull(state.rangeForm)
        assertEquals(listOf(LocalDate(2026, 12, 25)), state.blockedPeriods.map { it.date })
        assertFalse(state.isLoading)
    }
}
