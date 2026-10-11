package com.saif.remotelab

import com.saif.remotelab.data.DefaultInstruments
import com.saif.remotelab.data.InstrumentState
import com.saif.remotelab.data.NextInterval
import com.saif.remotelab.data.PlanRecord
import com.saif.remotelab.data.Run
import com.saif.remotelab.data.RunDraft
import com.saif.remotelab.data.RunField
import com.saif.remotelab.data.RunState
import com.saif.remotelab.data.StudyCondition
import com.saif.remotelab.data.TestType
import com.saif.remotelab.data.intervalLabel
import com.saif.remotelab.data.nextInterval
import com.saif.remotelab.data.toggleInterval
import com.saif.remotelab.data.validateRun
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class RulesTest {

    private val emptyBoard = DefaultInstruments.map { InstrumentState(it, running = null, queue = emptyList()) }

    private val draft = RunDraft(
        instrumentId = "Dwa36",
        productName = "Spectrabact",
        concentration = "625",
        batchNo = "B1",
        condition = StudyCondition.Accelerated,
        months = setOf(3),
        tests = setOf(TestType.Assay)
    )

    private fun record(months: Int, date: String, condition: StudyCondition = StudyCondition.LongTerm) = PlanRecord(
        productName = "Spectrabact",
        concentration = "625",
        batchNo = "B1",
        dateOfAnalysis = LocalDate.parse(date),
        condition = condition,
        months = months,
        test = TestType.Assay,
        monthPlan = YearMonth.from(LocalDate.parse(date))
    )

    @Test
    fun validDraftPasses() {
        assertTrue(validateRun(draft, emptyBoard).isEmpty())
    }

    @Test
    fun emptyDraftReportsEveryMissingField() {
        assertEquals(
            setOf(
                RunField.Product, RunField.Concentration, RunField.Batch,
                RunField.Interval, RunField.Instrument, RunField.Tests
            ),
            validateRun(RunDraft(), emptyBoard).keys
        )
    }

    @Test
    fun validationStudyNeedsNoInterval() {
        val problems = validateRun(draft.copy(condition = StudyCondition.Validation, months = emptySet()), emptyBoard)

        assertTrue(problems.isEmpty())
    }

    @Test
    fun testMustMatchInstrumentKind() {
        assertEquals(setOf(RunField.Tests), validateRun(draft.copy(instrumentId = "GC"), emptyBoard).keys)
        assertTrue(validateRun(draft.copy(instrumentId = "GC", tests = setOf(TestType.ResidualSolvents)), emptyBoard).isEmpty())
    }

    @Test
    fun onlyThreeAndSixMonthsCanBeCombined() {
        assertTrue(validateRun(draft.copy(months = setOf(3, 6)), emptyBoard).isEmpty())
        assertEquals(setOf(RunField.Interval), validateRun(draft.copy(months = setOf(1, 3)), emptyBoard).keys)
        assertEquals(
            setOf(RunField.Interval),
            validateRun(draft.copy(condition = StudyCondition.LongTerm, months = setOf(3, 6, 9)), emptyBoard).keys
        )
    }

    @Test
    fun intervalMustBelongToTheStudyType() {
        assertEquals(setOf(RunField.Interval), validateRun(draft.copy(months = setOf(9)), emptyBoard).keys)
        assertTrue(validateRun(draft.copy(condition = StudyCondition.LongTerm, months = setOf(9)), emptyBoard).isEmpty())
        assertTrue(validateRun(draft.copy(months = setOf(1)), emptyBoard).isEmpty())
    }

    @Test
    fun togglingIntervalsKeepsTheSelectionValid() {
        assertEquals(setOf(3), toggleInterval(emptySet(), 3))
        assertEquals(setOf(3, 6), toggleInterval(setOf(3), 6))
        assertEquals(setOf(6), toggleInterval(setOf(3, 6), 3))
        assertEquals(setOf(9), toggleInterval(setOf(3), 9))
        assertEquals(setOf(12), toggleInterval(setOf(3, 6), 12))
    }

    @Test
    fun combinedIntervalsShareOneLabel() {
        assertEquals("3M + 6M Acc", intervalLabel(StudyCondition.Accelerated, setOf(6, 3)))
        assertEquals("Validation", intervalLabel(StudyCondition.Validation, emptySet()))
    }

    @Test
    fun sameAnalysisCannotBeActiveTwice() {
        val active = Run(
            instrumentId = "Dwa37",
            productName = "spectrabact",
            concentration = "625",
            batchNo = "B1",
            condition = StudyCondition.Accelerated,
            months = setOf(3, 6),
            tests = setOf(TestType.Assay, TestType.Related),
            state = RunState.Queued,
            createdAt = 0
        )
        val board = emptyBoard.map { if (it.instrument.id == "Dwa37") it.copy(queue = listOf(active)) else it }

        assertEquals(setOf(RunField.Duplicate), validateRun(draft, board).keys)
        assertTrue(validateRun(draft.copy(months = setOf(1)), board).isEmpty())
    }

    @Test
    fun nextIntervalFollowsTheLatestRecord() {
        val history = listOf(record(0, "2026-01-10"), record(6, "2026-07-10"), record(3, "2026-04-10"))

        assertEquals(
            NextInterval(9, LocalDate.parse("2026-10-10")),
            nextInterval(StudyCondition.LongTerm, history)
        )
    }

    @Test
    fun nextIntervalIsNullWhenStudyIsComplete() {
        val history = listOf(record(6, "2026-07-10", StudyCondition.Accelerated))

        assertNull(nextInterval(StudyCondition.Accelerated, history))
        assertNull(nextInterval(StudyCondition.Validation, emptyList()))
    }
}
