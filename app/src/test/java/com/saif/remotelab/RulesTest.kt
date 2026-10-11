package com.saif.remotelab

import com.saif.remotelab.data.DefaultInstruments
import com.saif.remotelab.data.InstrumentState
import com.saif.remotelab.data.NextInterval
import com.saif.remotelab.data.PlanRecord
import com.saif.remotelab.data.Run
import com.saif.remotelab.data.RunDraft
import com.saif.remotelab.data.RunState
import com.saif.remotelab.data.StudyCondition
import com.saif.remotelab.data.TestType
import com.saif.remotelab.data.nextInterval
import com.saif.remotelab.data.validateRun
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
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
        months = 3,
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
        assertNull(validateRun(draft, emptyBoard))
    }

    @Test
    fun missingFieldsAreRejected() {
        assertNotNull(validateRun(draft.copy(productName = " "), emptyBoard))
        assertNotNull(validateRun(draft.copy(instrumentId = null), emptyBoard))
        assertNotNull(validateRun(draft.copy(tests = emptySet()), emptyBoard))
        assertNotNull(validateRun(draft.copy(months = null), emptyBoard))
    }

    @Test
    fun validationStudyNeedsNoInterval() {
        assertNull(validateRun(draft.copy(condition = StudyCondition.Validation, months = null), emptyBoard))
    }

    @Test
    fun testMustMatchInstrumentKind() {
        assertNotNull(validateRun(draft.copy(instrumentId = "GC"), emptyBoard))
        assertNull(validateRun(draft.copy(instrumentId = "GC", tests = setOf(TestType.ResidualSolvents)), emptyBoard))
    }

    @Test
    fun sameAnalysisCannotBeActiveTwice() {
        val active = Run(
            instrumentId = "Dwa37",
            productName = "spectrabact",
            concentration = "625",
            batchNo = "B1",
            condition = StudyCondition.Accelerated,
            months = 3,
            tests = setOf(TestType.Assay, TestType.Related),
            state = RunState.Queued,
            createdAt = 0
        )
        val board = emptyBoard.map { if (it.instrument.id == "Dwa37") it.copy(queue = listOf(active)) else it }

        assertNotNull(validateRun(draft, board))
        assertNull(validateRun(draft.copy(months = 6), board))
    }

    @Test
    fun nextIntervalFollowsTheLatestRecord() {
        val history = listOf(record(0, "2026-01-10"), record(6, "2026-07-10"), record(3, "2026-04-10"))

        assertEquals(
            NextInterval(12, LocalDate.parse("2027-01-10")),
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
