@file:OptIn(ExperimentalCoroutinesApi::class)

package corp.khin.solutions.booqi.feature.provider

import corp.khin.solutions.booqi.core.common.DomainError
import corp.khin.solutions.booqi.domain.model.DayHours
import corp.khin.solutions.booqi.domain.model.TimeRange
import corp.khin.solutions.booqi.domain.usecase.DefinirHorarioSemanalUseCase
import corp.khin.solutions.booqi.domain.usecase.ModificarHorarioSemanalUseCase
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
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalTime
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Grupo 3 scenarios (docs/domain/provider-flow.md) that surface on the P6 weekly editor,
 * exercised through [WeeklyScheduleViewModel]'s reducer: state and events out, for actions in.
 */
class WeeklyScheduleViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val repository = FakeAvailabilityRepository()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /** Builds the ViewModel and sends the Start [ScheduleManagementScreen] sends on every entry. */
    private fun newViewModel() = WeeklyScheduleViewModel(
        obtenerHorario = ObtenerHorarioUseCase(repository),
        definirHorario = DefinirHorarioSemanalUseCase(repository),
        modificarHorario = ModificarHorarioSemanalUseCase(repository),
    ).also { it.onAction(WeeklyScheduleAction.Start) }

    private fun hours(day: DayOfWeek, startHour: Int, endHour: Int, isActive: Boolean = true) =
        DayHours(day, isActive, TimeRange(LocalTime(startHour, 0), LocalTime(endHour, 0)))

    private fun WeeklyScheduleViewModel.row(day: DayOfWeek) = state.value.rows.single { it.day == day }

    /** Switches [day] on with [start]..[end] the way the row's toggle and fields do. */
    private fun WeeklyScheduleViewModel.fill(day: DayOfWeek, start: String, end: String) {
        onAction(WeeklyScheduleAction.DayToggled(day, isActive = true))
        onAction(WeeklyScheduleAction.StartChanged(day, start))
        onAction(WeeklyScheduleAction.EndChanged(day, end))
    }

    private suspend fun TestScope.awaitEventAfter(
        viewModel: WeeklyScheduleViewModel,
        action: WeeklyScheduleAction,
    ): WeeklyScheduleEvent {
        var event: WeeklyScheduleEvent? = null
        val collectorJob = launch { event = viewModel.events.first() }
        viewModel.onAction(action)
        advanceUntilIdle()
        collectorJob.join()
        return checkNotNull(event) { "No event received for $action" }
    }

    // --- Escenario: "El Proveedor consulta su horario" -------------------------------------------

    @Test
    fun `a provider without a schedule sees seven inactive rows Monday to Sunday and no error`() =
        runTest(testDispatcher) {
            val viewModel = newViewModel()

            advanceUntilIdle()

            val state = viewModel.state.value
            assertFalse(state.isLoading)
            assertNull(state.loadError)
            assertFalse(state.isScheduleDefined)
            assertEquals(DayOfWeek.entries, state.rows.map { it.day })
            assertTrue(state.rows.none { it.isActive })
            assertEquals("09:00", state.rows.first().startInput)
            assertEquals("18:00", state.rows.first().endInput)
        }

    @Test
    fun `an existing schedule shows its days in order with inactive and undefined days off`() =
        runTest(testDispatcher) {
            repository.seedWeekly(
                listOf(
                    hours(DayOfWeek.WEDNESDAY, 10, 14, isActive = false),
                    hours(DayOfWeek.MONDAY, 8, 12),
                ),
            )
            val viewModel = newViewModel()

            advanceUntilIdle()

            assertEquals(DayOfWeek.entries, viewModel.state.value.rows.map { it.day })
            assertEquals(listOf(true, false, false, false, false, false, false), viewModel.state.value.rows.map { it.isActive })
            assertEquals("08:00", viewModel.row(DayOfWeek.MONDAY).startInput)
            assertEquals("12:00", viewModel.row(DayOfWeek.MONDAY).endInput)
            // An inactive day keeps the range it had.
            assertEquals("10:00", viewModel.row(DayOfWeek.WEDNESDAY).startInput)
            assertTrue(viewModel.state.value.isScheduleDefined)
            assertTrue(viewModel.state.value.isSaved)
        }

    @Test
    fun `the first frame before the entry Start is loading and not an empty editor`() {
        val viewModel = WeeklyScheduleViewModel(
            obtenerHorario = ObtenerHorarioUseCase(repository),
            definirHorario = DefinirHorarioSemanalUseCase(repository),
            modificarHorario = ModificarHorarioSemanalUseCase(repository),
        )

        assertTrue(viewModel.state.value.isLoading)
    }

    // --- Escenario: "El Proveedor define su horario semanal" --------------------------------------

    @Test
    fun `the first save defines the whole week`() = runTest(testDispatcher) {
        val viewModel = newViewModel()
        advanceUntilIdle()
        viewModel.fill(DayOfWeek.MONDAY, "09:00", "12:00")
        viewModel.fill(DayOfWeek.TUESDAY, "10:00", "16:30")

        viewModel.onAction(WeeklyScheduleAction.Save)
        advanceUntilIdle()

        val stored = repository.stored().weeklyHours
        // Define sets the whole week: the five untouched days are stored as inactive too.
        assertEquals(DayOfWeek.entries, stored.map { it.day })
        assertEquals(
            listOf(true, true, false, false, false, false, false),
            stored.map { it.isActive },
        )
        assertEquals(TimeRange(LocalTime(10, 0), LocalTime(16, 30)), stored[1].hours)
        val state = viewModel.state.value
        assertFalse(state.isSaving)
        assertTrue(state.isScheduleDefined)
        assertTrue(state.isSaved)
        assertTrue(state.rows.all { it.error == null })
        assertNull(state.formError)
        assertEquals(1, repository.writeCount)
    }

    @Test
    fun `the first save works with every day off`() = runTest(testDispatcher) {
        val viewModel = newViewModel()
        advanceUntilIdle()

        viewModel.onAction(WeeklyScheduleAction.Save)
        advanceUntilIdle()

        assertEquals(7, repository.stored().weeklyHours.size)
        assertTrue(repository.stored().weeklyHours.none { it.isActive })
        assertTrue(viewModel.state.value.isSaved)
    }

    // --- Escenario: "El Proveedor modifica su horario semanal" ------------------------------------

    @Test
    fun `once a schedule exists a save modifies only the changed days`() = runTest(testDispatcher) {
        repository.seedWeekly(
            listOf(hours(DayOfWeek.MONDAY, 9, 12), hours(DayOfWeek.TUESDAY, 9, 12), hours(DayOfWeek.WEDNESDAY, 9, 12)),
        )
        val viewModel = newViewModel()
        advanceUntilIdle()

        viewModel.onAction(WeeklyScheduleAction.EndChanged(DayOfWeek.TUESDAY, "17:00"))
        assertTrue(viewModel.state.value.hasChanges)
        viewModel.onAction(WeeklyScheduleAction.Save)
        advanceUntilIdle()

        val stored = repository.stored().weeklyHours
        // A full-week Define would have stored seven days; Modify leaves the other days alone.
        assertEquals(listOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY), stored.map { it.day })
        assertEquals(LocalTime(17, 0), stored[1].hours.end)
        assertEquals(LocalTime(12, 0), stored[0].hours.end)
        assertTrue(viewModel.state.value.isSaved)
        assertEquals("17:00", viewModel.row(DayOfWeek.TUESDAY).endInput)
    }

    @Test
    fun `a save right after the first save is already a modify`() = runTest(testDispatcher) {
        val viewModel = newViewModel()
        advanceUntilIdle()
        viewModel.fill(DayOfWeek.MONDAY, "09:00", "12:00")
        viewModel.onAction(WeeklyScheduleAction.Save)
        advanceUntilIdle()
        assertEquals(7, repository.stored().weeklyHours.size)

        // Another device removes the untouched days; a stale full-week Define would bring them back.
        repository.seedWeekly(listOf(hours(DayOfWeek.MONDAY, 9, 12)))
        viewModel.onAction(WeeklyScheduleAction.EndChanged(DayOfWeek.MONDAY, "13:00"))
        viewModel.onAction(WeeklyScheduleAction.Save)
        advanceUntilIdle()

        assertEquals(listOf(DayOfWeek.MONDAY), repository.stored().weeklyHours.map { it.day })
        assertEquals(LocalTime(13, 0), repository.stored().weeklyHours.single().hours.end)
    }

    @Test
    fun `turning a day off keeps its range and a save stores it inactive`() = runTest(testDispatcher) {
        repository.seedWeekly(listOf(hours(DayOfWeek.MONDAY, 9, 12), hours(DayOfWeek.FRIDAY, 9, 12)))
        val viewModel = newViewModel()
        advanceUntilIdle()

        viewModel.onAction(WeeklyScheduleAction.DayToggled(DayOfWeek.FRIDAY, isActive = false))
        viewModel.onAction(WeeklyScheduleAction.Save)
        advanceUntilIdle()

        val friday = repository.stored().weeklyHours.single { it.day == DayOfWeek.FRIDAY }
        assertFalse(friday.isActive)
        assertEquals(TimeRange(LocalTime(9, 0), LocalTime(12, 0)), friday.hours)
    }

    @Test
    fun `saving an unchanged schedule sends nothing`() = runTest(testDispatcher) {
        repository.seedWeekly(listOf(hours(DayOfWeek.MONDAY, 9, 12)))
        val viewModel = newViewModel()
        advanceUntilIdle()

        viewModel.onAction(WeeklyScheduleAction.Save)
        advanceUntilIdle()

        assertEquals(0, repository.writeCount)
        assertFalse(viewModel.state.value.hasChanges)
    }

    @Test
    fun `an inactive day with unusable text neither blocks the save nor changes the stored hours`() =
        runTest(testDispatcher) {
            repository.seedWeekly(listOf(hours(DayOfWeek.MONDAY, 9, 12), hours(DayOfWeek.TUESDAY, 9, 12, isActive = false)))
            val viewModel = newViewModel()
            advanceUntilIdle()

            viewModel.onAction(WeeklyScheduleAction.StartChanged(DayOfWeek.TUESDAY, "basura"))
            viewModel.onAction(WeeklyScheduleAction.EndChanged(DayOfWeek.MONDAY, "13:00"))
            viewModel.onAction(WeeklyScheduleAction.Save)
            advanceUntilIdle()

            assertEquals(1, repository.writeCount)
            assertEquals(LocalTime(13, 0), repository.stored().weeklyHours.first().hours.end)
            assertTrue(viewModel.state.value.rows.all { it.error == null })
        }

    @Test
    fun `modifying when the schedule has meanwhile disappeared reports there is nothing to modify`() =
        runTest(testDispatcher) {
            repository.seedWeekly(listOf(hours(DayOfWeek.MONDAY, 9, 12)))
            val viewModel = newViewModel()
            advanceUntilIdle()
            repository.seedWeekly(emptyList()) // gone on the other side
            viewModel.onAction(WeeklyScheduleAction.EndChanged(DayOfWeek.MONDAY, "13:00"))

            val event = awaitEventAfter(viewModel, WeeklyScheduleAction.Save)

            assertEquals(WeeklyScheduleEvent.ShowError("No hay un horario que modificar"), event)
            assertEquals("No hay un horario que modificar", viewModel.state.value.formError)
            assertEquals(0, repository.writeCount)
            assertEquals(0, repository.stored().weeklyHours.size)
            assertFalse(viewModel.state.value.isSaving)
        }

    // --- Escenario: "...define o modifica un horario con horas inválidas" -------------------------

    @Test
    fun `an active day ending at or before its start is rejected on that row and nothing is saved`() =
        runTest(testDispatcher) {
            repository.seedWeekly(listOf(hours(DayOfWeek.MONDAY, 9, 12)))
            val viewModel = newViewModel()
            advanceUntilIdle()
            viewModel.fill(DayOfWeek.TUESDAY, "10:00", "10:00")
            viewModel.fill(DayOfWeek.WEDNESDAY, "10:00", "09:00")
            viewModel.fill(DayOfWeek.THURSDAY, "10:00", "11:00")

            viewModel.onAction(WeeklyScheduleAction.Save)
            advanceUntilIdle()

            val message = "La hora de fin debe ser posterior a la hora de inicio"
            assertEquals(message, viewModel.row(DayOfWeek.TUESDAY).error)
            assertEquals(message, viewModel.row(DayOfWeek.WEDNESDAY).error)
            assertNull(viewModel.row(DayOfWeek.THURSDAY).error)
            assertNull(viewModel.row(DayOfWeek.MONDAY).error)
            assertNull(viewModel.state.value.formError)
            assertFalse(viewModel.state.value.isSaving)
            // "el horario guardado no cambia"
            assertEquals(0, repository.writeCount)
            assertEquals(listOf(DayOfWeek.MONDAY), repository.stored().weeklyHours.map { it.day })
        }

    @Test
    fun `an invalid first definition is rejected on the row and the day stays unsaved`() = runTest(testDispatcher) {
        val viewModel = newViewModel()
        advanceUntilIdle()
        viewModel.fill(DayOfWeek.MONDAY, "18:00", "09:00")

        viewModel.onAction(WeeklyScheduleAction.Save)
        advanceUntilIdle()

        assertEquals("La hora de fin debe ser posterior a la hora de inicio", viewModel.row(DayOfWeek.MONDAY).error)
        assertEquals(0, repository.writeCount)
        assertFalse(viewModel.state.value.isScheduleDefined)
    }

    @Test
    fun `text that is not a time is a row error and never reaches the use case`() = runTest(testDispatcher) {
        val viewModel = newViewModel()
        advanceUntilIdle()
        viewModel.fill(DayOfWeek.MONDAY, "nueve", "12:00")
        viewModel.fill(DayOfWeek.TUESDAY, "09:00", "")

        viewModel.onAction(WeeklyScheduleAction.Save)
        advanceUntilIdle()

        assertEquals(INVALID_TIME_MESSAGE, viewModel.row(DayOfWeek.MONDAY).error)
        assertEquals(INVALID_TIME_MESSAGE, viewModel.row(DayOfWeek.TUESDAY).error)
        assertEquals(0, repository.writeCount)
        assertTrue(viewModel.state.value.hasChanges, "Save must stay available to show the errors")
    }

    @Test
    fun `editing a row clears its error but not the others`() = runTest(testDispatcher) {
        val viewModel = newViewModel()
        advanceUntilIdle()
        viewModel.fill(DayOfWeek.MONDAY, "nueve", "12:00")
        viewModel.fill(DayOfWeek.TUESDAY, "nueve", "12:00")
        viewModel.onAction(WeeklyScheduleAction.Save)
        advanceUntilIdle()

        viewModel.onAction(WeeklyScheduleAction.StartChanged(DayOfWeek.MONDAY, "09:00"))

        assertNull(viewModel.row(DayOfWeek.MONDAY).error)
        assertEquals(INVALID_TIME_MESSAGE, viewModel.row(DayOfWeek.TUESDAY).error)
    }

    @Test
    fun `a valid correction after an error saves`() = runTest(testDispatcher) {
        val viewModel = newViewModel()
        advanceUntilIdle()
        viewModel.fill(DayOfWeek.MONDAY, "18:00", "09:00")
        viewModel.onAction(WeeklyScheduleAction.Save)
        advanceUntilIdle()
        assertEquals(0, repository.writeCount)

        viewModel.onAction(WeeklyScheduleAction.StartChanged(DayOfWeek.MONDAY, "08:00"))
        viewModel.onAction(WeeklyScheduleAction.Save)
        advanceUntilIdle()

        assertEquals(1, repository.writeCount)
        assertTrue(viewModel.state.value.isSaved)
        assertTrue(viewModel.state.value.rows.all { it.error == null })
    }

    @Test
    fun `a double tap on Save and edits made mid-save do not double write or get lost`() = runTest(testDispatcher) {
        val viewModel = newViewModel()
        advanceUntilIdle()
        viewModel.fill(DayOfWeek.MONDAY, "09:00", "12:00")

        viewModel.onAction(WeeklyScheduleAction.Save)
        viewModel.onAction(WeeklyScheduleAction.Save)
        assertTrue(viewModel.state.value.isSaving)
        viewModel.onAction(WeeklyScheduleAction.EndChanged(DayOfWeek.MONDAY, "20:00")) // ignored: mid-save
        advanceUntilIdle()

        assertEquals(1, repository.writeCount)
        assertEquals("12:00", viewModel.row(DayOfWeek.MONDAY).endInput)
        assertTrue(viewModel.state.value.isSaved)
    }

    // --- Failures ---------------------------------------------------------------------------------

    @Test
    fun `a failing save keeps the edits and shows the failure without crashing`() = runTest(testDispatcher) {
        val viewModel = newViewModel()
        advanceUntilIdle()
        viewModel.fill(DayOfWeek.MONDAY, "09:00", "12:00")
        repository.writeFailure = DomainError.NoConnection

        val event = awaitEventAfter(viewModel, WeeklyScheduleAction.Save)

        assertEquals(WeeklyScheduleEvent.ShowError("Sin conexión"), event)
        val state = viewModel.state.value
        assertEquals("Sin conexión", state.formError)
        assertFalse(state.isSaving)
        assertTrue(viewModel.row(DayOfWeek.MONDAY).isActive)
        assertFalse(state.isScheduleDefined)
    }

    @Test
    fun `a failing load exposes the error and a Start after it recovers`() = runTest(testDispatcher) {
        repository.seedWeekly(listOf(hours(DayOfWeek.MONDAY, 9, 12)))
        repository.readFailure = DomainError.NoConnection
        val viewModel = newViewModel()
        advanceUntilIdle() // the ShowError send stays suspended (no collector)
        assertEquals(DomainError.NoConnection, viewModel.state.value.loadError)
        assertFalse(viewModel.state.value.isLoading)

        repository.readFailure = null
        viewModel.onAction(WeeklyScheduleAction.Start) // the "Reintentar" button
        advanceUntilIdle()

        assertNull(viewModel.state.value.loadError)
        assertTrue(viewModel.row(DayOfWeek.MONDAY).isActive)
    }

    @Test
    fun `save is ignored while the schedule failed to load`() = runTest(testDispatcher) {
        repository.readFailure = DomainError.Timeout
        val viewModel = newViewModel()
        advanceUntilIdle()

        viewModel.onAction(WeeklyScheduleAction.Save)
        advanceUntilIdle()

        assertEquals(0, repository.writeCount)
    }

    // --- Re-entry (ViewModels are not destination-scoped) -----------------------------------------

    @Test
    fun `re-entering shows fresh data and drops unsaved edits`() = runTest(testDispatcher) {
        repository.seedWeekly(listOf(hours(DayOfWeek.MONDAY, 9, 12)))
        val viewModel = newViewModel()
        advanceUntilIdle()
        viewModel.fill(DayOfWeek.SATURDAY, "10:00", "14:00") // left unsaved when the screen was left
        repository.seedWeekly(listOf(hours(DayOfWeek.MONDAY, 8, 20))) // changed elsewhere meanwhile

        viewModel.onAction(WeeklyScheduleAction.Start) // what ScheduleManagementScreen sends on entry
        advanceUntilIdle()

        assertFalse(viewModel.row(DayOfWeek.SATURDAY).isActive)
        assertEquals("08:00", viewModel.row(DayOfWeek.MONDAY).startInput)
        assertEquals("20:00", viewModel.row(DayOfWeek.MONDAY).endInput)
        assertFalse(viewModel.state.value.hasChanges)
        assertTrue(viewModel.state.value.rows.all { it.error == null })
    }

    @Test
    fun `re-entering after a first save shows the saved schedule`() = runTest(testDispatcher) {
        val viewModel = newViewModel()
        advanceUntilIdle()
        viewModel.fill(DayOfWeek.MONDAY, "09:00", "12:00")
        viewModel.onAction(WeeklyScheduleAction.Save)
        advanceUntilIdle()

        viewModel.onAction(WeeklyScheduleAction.Start)
        advanceUntilIdle()

        assertTrue(viewModel.row(DayOfWeek.MONDAY).isActive)
        assertTrue(viewModel.state.value.isSaved)
    }
}
